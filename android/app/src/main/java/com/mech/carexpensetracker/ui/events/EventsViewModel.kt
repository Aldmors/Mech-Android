package com.mech.carexpensetracker.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.data.repository.NoteRepository
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.MileageValidationService
import com.mech.carexpensetracker.domain.service.NoteSortingService
import com.mech.carexpensetracker.domain.service.RecordCostService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

enum class EventFilter {
    All, Fuel, Service, Documents, Notes,
}

data class EventsUiState(
    val events: List<CarEventEntity> = emptyList(),
    val notes: List<CarNoteEntity> = emptyList(),
    val filter: EventFilter = EventFilter.All,
    val searchQuery: String = "",
    val showingNotes: Boolean = false,
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(EventFilter.All)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<EventsUiState> = combine(
        carRepository.observeSelectedCar().flatMapLatest { car ->
            if (car == null) flowOf(emptyList<CarEventEntity>() to emptyList<CarNoteEntity>())
            else combine(
                eventRepository.observeEvents(car.externalId),
                noteRepository.observeNotes(car.externalId),
            ) { events, notes -> events to notes }
        },
        filter,
        searchQuery,
    ) { data, currentFilter, query ->
        val (events, notes) = data
        val filteredEvents = when (currentFilter) {
            EventFilter.All -> events
            EventFilter.Fuel -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Fuel }
            EventFilter.Service -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Repair }
            EventFilter.Documents -> events.filter { EventType.fromRaw(it.typeRaw) == EventType.Papers }
            EventFilter.Notes -> events
        }.filter { event ->
            query.isBlank() || event.comment?.contains(query, ignoreCase = true) == true ||
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
        mileage: Int?,
        fuelAmount: BigDecimal?,
        fuelCost: BigDecimal?,
        secondaryFuelAmount: BigDecimal?,
        secondaryFuelCost: BigDecimal?,
        fullTank: Boolean,
        secondaryFullTank: Boolean,
        comment: String?,
    ) {
        val existing = eventRepository.getEvents(carExternalId)
        val validation = MileageValidationService.validate(mileage, existing)
        if (!validation.isValid) return

        val total = RecordCostService.totalCost(fuelCost, secondaryFuelCost, null, null, null)
        val event = CarEventEntity(
            externalId = UUID.randomUUID().toString(),
            carExternalId = carExternalId,
            typeRaw = EventType.Fuel.raw,
            dateMillis = System.currentTimeMillis(),
            mileage = mileage,
            comment = comment,
            totalCost = total?.toPlainString(),
            fuelTypeRaw = primaryFuelType,
            fuelAmount = fuelAmount?.toPlainString(),
            fuelCost = fuelCost?.toPlainString(),
            fuelFullTank = fullTank,
            secondaryFuelTypeRaw = altFuelType,
            secondaryFuelAmount = secondaryFuelAmount?.toPlainString(),
            secondaryFuelCost = secondaryFuelCost?.toPlainString(),
            secondaryFuelFullTank = secondaryFullTank,
        )
        eventRepository.upsertEvent(event)
    }

    suspend fun addExpenseEvent(
        carExternalId: String,
        type: EventType,
        mileage: Int?,
        categoryName: String?,
        partsCost: BigDecimal?,
        labourCost: BigDecimal?,
        comment: String?,
    ) {
        val existing = eventRepository.getEvents(carExternalId)
        val validation = MileageValidationService.validate(mileage, existing)
        if (!validation.isValid) return

        val total = RecordCostService.repairTotal(partsCost, labourCost)
        val event = CarEventEntity(
            externalId = UUID.randomUUID().toString(),
            carExternalId = carExternalId,
            typeRaw = type.raw,
            dateMillis = System.currentTimeMillis(),
            mileage = mileage,
            comment = comment,
            totalCost = total?.toPlainString(),
            categoryName = categoryName,
            partsCost = partsCost?.toPlainString(),
            labourCost = labourCost?.toPlainString(),
        )
        eventRepository.upsertEvent(event)
    }

    fun deleteEvent(externalId: String) {
        viewModelScope.launch { eventRepository.deleteEvent(externalId) }
    }

    fun formatCost(event: CarEventEntity): String =
        CurrencyFormatter.formatOrDash(event.totalCost?.toBigDecimalOrNull())
}
