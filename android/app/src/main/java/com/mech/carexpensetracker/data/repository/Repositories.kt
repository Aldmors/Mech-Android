package com.mech.carexpensetracker.data.repository

import android.net.Uri
import com.mech.carexpensetracker.data.EventPhotoStore
import com.mech.carexpensetracker.data.db.CarDao
import com.mech.carexpensetracker.data.db.CarEventDao
import com.mech.carexpensetracker.data.db.CarNoteDao
import com.mech.carexpensetracker.data.db.CarReminderDao
import com.mech.carexpensetracker.data.db.EventPhotoDao
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import com.mech.carexpensetracker.data.prefs.SelectedCarStore
import com.mech.carexpensetracker.domain.service.ObligatoryReminderService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID

class CarRepository(
    private val carDao: CarDao,
    private val carEventDao: CarEventDao,
    private val carReminderDao: CarReminderDao,
    private val selectedCarStore: SelectedCarStore,
) {
    fun observeCars(): Flow<List<CarEntity>> = carDao.observeAll()

    suspend fun getCars(): List<CarEntity> = carDao.getAll()

    fun observeSelectedCar(): Flow<CarEntity?> = combine(
        carDao.observeAll(),
        selectedCarStore.selectedCarExternalId,
    ) { cars, selectedId ->
        when {
            cars.isEmpty() -> null
            selectedId != null -> cars.find { it.externalId == selectedId } ?: cars.first()
            else -> cars.first()
        }
    }

    suspend fun getCar(externalId: String): CarEntity? = carDao.getByExternalId(externalId)

    suspend fun upsertCar(car: CarEntity) {
        val existing = carDao.getByExternalId(car.externalId)
        carDao.upsert(car)
        if (existing == null) {
            ObligatoryReminderService.seedForCar(car.externalId).forEach { carReminderDao.upsert(it) }
        }
    }

    suspend fun deleteCar(externalId: String) {
        carDao.deleteByExternalId(externalId)
        val remaining = carDao.getAll()
        val selectedId = selectedCarStore.selectedCarExternalId.first()
        when {
            remaining.isEmpty() -> selectedCarStore.setSelectedCar(null)
            selectedId == null || remaining.none { it.externalId == selectedId } -> {
                selectedCarStore.setSelectedCar(remaining.first().externalId)
            }
        }
    }

    suspend fun selectCar(externalId: String) {
        selectedCarStore.setSelectedCar(externalId)
    }

    suspend fun createCar(
        name: String,
        plateNumber: String?,
        vehicleUnits: String,
        primaryFuelType: String,
        alternativeFuelType: String?,
        iconName: String,
        buyDateMillis: Long? = null,
    ): CarEntity {
        val car = CarEntity(
            externalId = UUID.randomUUID().toString(),
            name = name,
            plateNumber = plateNumber,
            vehicleUnits = vehicleUnits,
            primaryFuelTypeRaw = primaryFuelType,
            alternativeFuelTypeRaw = alternativeFuelType,
            iconName = iconName,
            buyDateMillis = buyDateMillis,
        )
        upsertCar(car)
        selectCar(car.externalId)
        return car
    }
}

class EventRepository(
    private val carEventDao: CarEventDao,
) {
    fun observeEvents(carExternalId: String): Flow<List<CarEventEntity>> =
        carEventDao.observeForCar(carExternalId)

    fun observeAll(): Flow<List<CarEventEntity>> = carEventDao.observeAll()

    suspend fun getEvents(carExternalId: String): List<CarEventEntity> =
        carEventDao.getForCar(carExternalId)

    suspend fun getAll(): List<CarEventEntity> = carEventDao.getAll()

    suspend fun getEvent(externalId: String): CarEventEntity? =
        carEventDao.getByExternalId(externalId)

    suspend fun upsertEvent(event: CarEventEntity) {
        carEventDao.upsert(event)
    }

    suspend fun deleteEvent(externalId: String) {
        carEventDao.deleteByExternalId(externalId)
    }
}

class ReminderRepository(
    private val carReminderDao: CarReminderDao,
) {
    fun observeReminders(carExternalId: String): Flow<List<CarReminderEntity>> =
        carReminderDao.observeForCar(carExternalId)

    fun observeAll(): Flow<List<CarReminderEntity>> = carReminderDao.observeAll()

    suspend fun getAll(): List<CarReminderEntity> = carReminderDao.getAll()

    suspend fun getForCar(carExternalId: String): List<CarReminderEntity> =
        carReminderDao.getForCar(carExternalId)

    suspend fun get(externalId: String): CarReminderEntity? =
        carReminderDao.getByExternalId(externalId)

    suspend fun upsert(reminder: CarReminderEntity) {
        carReminderDao.upsert(reminder)
    }

    suspend fun delete(externalId: String) {
        carReminderDao.deleteByExternalId(externalId)
    }
}

class EventPhotoRepository(
    private val eventPhotoDao: EventPhotoDao,
    private val photoStore: EventPhotoStore,
) {
    fun observeAll(): Flow<List<EventPhotoEntity>> = eventPhotoDao.observeAll()

    fun observeForEvent(eventExternalId: String): Flow<List<EventPhotoEntity>> =
        eventPhotoDao.observeForEvent(eventExternalId)

    suspend fun copyFromUri(uri: Uri): EventPhotoEntity? {
        val id = UUID.randomUUID().toString()
        val path = photoStore.saveFromUri(id, uri) ?: return null
        return EventPhotoEntity(
            externalId = id,
            eventExternalId = "",
            imagePath = path,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    suspend fun commit(
        eventExternalId: String,
        keepIds: Set<String>,
        pending: List<EventPhotoEntity>,
    ) {
        eventPhotoDao.getForEvent(eventExternalId).forEach { photo ->
            if (photo.externalId !in keepIds) delete(photo)
        }
        pending.forEach { photo ->
            eventPhotoDao.upsert(photo.copy(id = 0, eventExternalId = eventExternalId))
        }
    }

    suspend fun delete(photo: EventPhotoEntity) {
        photoStore.delete(photo.imagePath)
        eventPhotoDao.deleteByExternalId(photo.externalId)
    }

    suspend fun deleteForEvent(eventExternalId: String) {
        eventPhotoDao.getForEvent(eventExternalId).forEach { delete(it) }
    }

    fun file(photo: EventPhotoEntity): File = photoStore.fileFor(photo.imagePath)

    fun discardFile(photo: EventPhotoEntity) {
        photoStore.delete(photo.imagePath)
    }
}

class NoteRepository(
    private val carNoteDao: CarNoteDao,
) {
    fun observeNotes(carExternalId: String): Flow<List<CarNoteEntity>> =
        carNoteDao.observeForCar(carExternalId)

    suspend fun upsert(note: CarNoteEntity) {
        carNoteDao.upsert(note)
    }
}

