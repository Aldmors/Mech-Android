package com.mech.carexpensetracker.data.repository

import com.mech.carexpensetracker.data.db.CarDao
import com.mech.carexpensetracker.data.db.CarEventDao
import com.mech.carexpensetracker.data.db.CarNoteDao
import com.mech.carexpensetracker.data.db.CarReminderDao
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.prefs.SelectedCarStore
import com.mech.carexpensetracker.domain.service.ObligatoryReminderService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class CarRepository(
    private val carDao: CarDao,
    private val carEventDao: CarEventDao,
    private val carReminderDao: CarReminderDao,
    private val selectedCarStore: SelectedCarStore,
) {
    fun observeCars(): Flow<List<CarEntity>> = carDao.observeAll()

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
    ): CarEntity {
        val car = CarEntity(
            externalId = UUID.randomUUID().toString(),
            name = name,
            plateNumber = plateNumber,
            vehicleUnits = vehicleUnits,
            primaryFuelTypeRaw = primaryFuelType,
            alternativeFuelTypeRaw = alternativeFuelType,
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

    suspend fun getEvents(carExternalId: String): List<CarEventEntity> =
        carEventDao.getForCar(carExternalId)

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

    suspend fun upsert(reminder: CarReminderEntity) {
        carReminderDao.upsert(reminder)
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
