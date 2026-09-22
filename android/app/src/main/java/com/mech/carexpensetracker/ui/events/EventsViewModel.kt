package com.mech.carexpensetracker.ui.events

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventPhotoRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.data.repository.NoteRepository
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.service.ConsumptionCalculator
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.MileageValidationService
import com.mech.carexpensetracker.domain.service.NoteSortingService
import com.mech.carexpensetracker.domain.service.RecordCostService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

enum class EventFilter {
    All, Fuel, Service, Documents, Care, Notes,
}

data class EventsUiState(
    val events: List<CarEventEntity> = emptyList(),
    val notes: List<CarNoteEntity> = emptyList(),
    val filter: EventFilter = EventFilter.All,
    val searchQuery: String = "",
    val showingNotes: Boolean = false,
    val vehicleUnits: VehicleUnits = VehicleUnits.Km,
    val consumptionById: Map<String, java.math.BigDecimal> = emptyMap(),
    val photoEventIds: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val photoRepository: EventPhotoRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(EventFilter.All)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<EventsUiState> = combine(
        carRepository.observeSelectedCar().flatMapLatest { car ->
            if (car == null) {
                flowOf(Triple(emptyList<CarEventEntity>(), emptyList<CarNoteEntity>(), VehicleUnits.Km))
            } else {
                combine(
                    eventRepository.observeEvents(car.externalId),
                    noteRepository.observeNotes(car.externalId),
                ) { events, notes -> Triple(events, notes, VehicleUnits.fromRaw(car.vehicleUnits)) }
            }
        },
        filter,
        searchQuery,
        photoRepository.observeAll(),
    ) { data, currentFilter, query, photos ->
        val (events, notes, units) = data
        val filteredEvents = when (currentFilter) {
            EventFilter.All -> events
            EventFilter.Fuel -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Fuel }
            EventFilter.Service -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Repair }
            EventFilter.Care -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Care }
            EventFilter.Documents -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Papers }
            EventFilter.Notes -> events
        }.filter { event ->
            query.isBlank() || event.comment?.contains(query, ignoreCase = true) == true ||
                event.name?.contains(query, ignoreCase = true) == true ||
                event.categoryName?.contains(query, ignoreCase = true) == true
        }
        EventsUiState(
            events = filteredEvents,
            notes = NoteSortingService.sort(notes).filter { note ->
                query.isBlank() || note.title.contains(query, ignoreCase = true) ||
                    note.details?.contains(query, ignoreCase = true) == true
            },
            filter = currentFilter,
            searchQuery = query,
            showingNotes = currentFilter == EventFilter.Notes,
            vehicleUnits = units,
            consumptionById = ConsumptionCalculator.displayedConsumptionByEventId(events, units),
            photoEventIds = photos.map { it.eventExternalId }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventsUiState())

    fun setFilter(filter: EventFilter) {
        this.filter.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    suspend fun addFuelEvent(
        carExternalId: String,
        primaryFuelType: String,
        altFuelType: String?,
        isAlternativeFuel: Boolean,
        mileage: Int?,
        fuelAmount: BigDecimal?,
        fuelCost: BigDecimal?,
        fullTank: Boolean,
        comment: String?,
        name: String? = null,
        dateMillis: Long = System.currentTimeMillis(),
        eventId: String? = null,
    ): String? {
        val previous = eventId?.let { eventRepository.getEvent(it) }
        val existing = eventRepository.getEvents(carExternalId)
        val validation = MileageValidationService.validate(
            mileage,
            existing,
            excludeExternalId = eventId,
            dateMillis = dateMillis,
        )
        if (!validation.isValid) return null

        val amountText = fuelAmount?.toPlainString()
        val costText = fuelCost?.toPlainString()
        val combined = previous != null &&
            !previous.fuelAmount.isNullOrBlank() &&
            !previous.secondaryFuelAmount.isNullOrBlank()
        val total = RecordCostService.totalCost(
            fuelCost = if (!isAlternativeFuel) {
                fuelCost
            } else {
                CurrencyFormatter.parseStored(previous?.fuelCost)
            },
            secondaryFuelCost = if (isAlternativeFuel) {
                fuelCost
            } else {
                CurrencyFormatter.parseStored(previous?.secondaryFuelCost)
            },
            partsCost = null,
            labourCost = null,
            explicitTotal = null,
        )
        val event = (previous ?: CarEventEntity(
            externalId = eventId ?: UUID.randomUUID().toString(),
            carExternalId = carExternalId,
            typeRaw = EventType.Fuel.raw,
            dateMillis = dateMillis,
        )).copy(
            dateMillis = dateMillis,
            mileage = mileage,
            comment = comment,
            name = name,
            totalCost = total?.toPlainString(),
            fuelTypeRaw = when {
                !isAlternativeFuel -> primaryFuelType
                combined -> previous?.fuelTypeRaw
                else -> null
            },
            fuelAmount = when {
                !isAlternativeFuel -> amountText
                combined -> previous?.fuelAmount
                else -> null
            },
            fuelCost = when {
                !isAlternativeFuel -> costText
                combined -> previous?.fuelCost
                else -> null
            },
            fuelFullTank = when {
                !isAlternativeFuel -> fullTank
                combined -> previous?.fuelFullTank ?: false
                else -> false
            },
            secondaryFuelTypeRaw = when {
                isAlternativeFuel -> altFuelType
                combined -> previous?.secondaryFuelTypeRaw
                else -> null
            },
            secondaryFuelAmount = when {
                isAlternativeFuel -> amountText
                combined -> previous?.secondaryFuelAmount
                else -> null
            },
            secondaryFuelCost = when {
                isAlternativeFuel -> costText
                combined -> previous?.secondaryFuelCost
                else -> null
            },
            secondaryFuelFullTank = when {
                isAlternativeFuel -> fullTank
                combined -> previous?.secondaryFuelFullTank ?: false
                else -> false
            },
        )
        eventRepository.upsertEvent(event)
        return event.externalId
    }

    suspend fun addExpenseEvent(
        carExternalId: String,
        type: EventType,
        mileage: Int?,
        name: String? = null,
        partsCost: BigDecimal?,
        labourCost: BigDecimal?,
        totalCost: BigDecimal? = null,
        comment: String?,
        dateMillis: Long = System.currentTimeMillis(),
        eventId: String? = null,
    ): String? {
        val previous = eventId?.let { eventRepository.getEvent(it) }
        val existing = eventRepository.getEvents(carExternalId)
        val validation = MileageValidationService.validate(
            mileage,
            existing,
            excludeExternalId = eventId,
            dateMillis = dateMillis,
        )
        if (!validation.isValid) return null

        val total = RecordCostService.repairTotal(partsCost, labourCost, totalCost)
        val event = (previous ?: CarEventEntity(
            externalId = eventId ?: UUID.randomUUID().toString(),
            carExternalId = carExternalId,
            typeRaw = type.raw,
            dateMillis = dateMillis,
        )).copy(
            typeRaw = type.raw,
            dateMillis = dateMillis,
            mileage = mileage,
            comment = comment,
            totalCost = total?.toPlainString(),
            name = name,
            partsCost = partsCost?.toPlainString(),
            labourCost = labourCost?.toPlainString(),
        )
        eventRepository.upsertEvent(event)
        return event.externalId
    }

    suspend fun getEvent(externalId: String): CarEventEntity? =
        eventRepository.getEvent(externalId)

    fun observePhotos(eventId: String?): Flow<List<EventPhotoEntity>> =
        if (eventId.isNullOrBlank()) flowOf(emptyList()) else photoRepository.observeForEvent(eventId)

    fun photoFile(photo: EventPhotoEntity): File = photoRepository.file(photo)

    suspend fun copyPickedPhoto(uri: Uri): EventPhotoEntity? =
        withContext(Dispatchers.IO) { photoRepository.copyFromUri(uri) }

    suspend fun commitPhotos(
        eventExternalId: String,
        keepIds: Set<String>,
        pending: List<EventPhotoEntity>,
    ) {
        withContext(Dispatchers.IO) {
            photoRepository.commit(eventExternalId, keepIds, pending)
        }
    }

    fun discardPhotoFile(photo: EventPhotoEntity) {
        photoRepository.discardFile(photo)
    }

    fun deleteEvent(externalId: String) {
        viewModelScope.launch {
            photoRepository.deleteForEvent(externalId)
            eventRepository.deleteEvent(externalId)
        }
    }
}
