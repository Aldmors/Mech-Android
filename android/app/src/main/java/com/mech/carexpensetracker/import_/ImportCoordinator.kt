package com.mech.carexpensetracker.import_

import com.mech.carexpensetracker.data.db.AppDatabase
import com.mech.carexpensetracker.data.db.CarDao
import com.mech.carexpensetracker.data.db.CarEventDao
import com.mech.carexpensetracker.data.db.CarNoteDao
import com.mech.carexpensetracker.data.db.CarReminderDao
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.domain.service.ObligatoryReminderService

data class ImportPreview(
    val carCount: Int,
    val eventCount: Int,
    val reminderCount: Int,
    val noteCount: Int,
    val cars: List<CarEntity>,
)

enum class ImportMode {
    Merge,
    ReplacePerCar,
}

class ImportCoordinator(
    private val database: AppDatabase,
    private val carDao: CarDao,
    private val carEventDao: CarEventDao,
    private val carReminderDao: CarReminderDao,
    private val carNoteDao: CarNoteDao,
) {
    fun preview(files: Map<String, String>): ImportPreview {
        val garageRows = files[CarnotesDtos.GARAGE_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        val eventRows = files[CarnotesDtos.CAR_EVENTS_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        val reminderRows = files[CarnotesDtos.CAR_REMINDERS_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        val noteRows = files[CarnotesDtos.NOTES_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()

        val cars = CarnotesParser.parseGarage(garageRows)
        val events = CarnotesParser.parseEvents(eventRows)
        val reminders = CarnotesParser.parseReminders(reminderRows)
        val notes = CarnotesParser.parseNotes(noteRows)

        return ImportPreview(
            carCount = cars.size,
            eventCount = events.size,
            reminderCount = reminders.size,
            noteCount = notes.size,
            cars = cars,
        )
    }

    suspend fun import(
        files: Map<String, String>,
        mode: ImportMode = ImportMode.Merge,
        carOverrides: Map<String, CarEntity> = emptyMap(),
    ): ImportPreview {
        val preview = preview(files)
        val garageRows = files[CarnotesDtos.GARAGE_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        val eventRows = files[CarnotesDtos.CAR_EVENTS_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        val reminderRows = files[CarnotesDtos.CAR_REMINDERS_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        val noteRows = files[CarnotesDtos.NOTES_TABLE]?.let { CarnotesParser.parseTable(it) } ?: emptyList()

        val cars = CarnotesParser.parseGarage(garageRows).map { car ->
            carOverrides[car.externalId] ?: car
        }
        val events = CarnotesParser.parseEvents(eventRows)
        val reminders = CarnotesParser.parseReminders(reminderRows)
        val notes = CarnotesParser.parseNotes(noteRows)

        cars.forEach { car ->
            val existing = carDao.getByExternalId(car.externalId)
            carDao.upsert(car)
            if (existing == null) {
                ObligatoryReminderService.seedForCar(car.externalId).forEach { carReminderDao.upsert(it) }
            }
        }

        if (mode == ImportMode.ReplacePerCar) {
            cars.forEach { car ->
                val carEvents = events.filter { it.carExternalId == car.externalId }
                val carReminders = reminders.filter { it.carExternalId == car.externalId }
                val carNotes = notes.filter { it.carExternalId == car.externalId }
                database.replaceCarData(car.externalId, carEvents, carReminders, carNotes)
            }
        } else {
            events.forEach { carEventDao.upsert(it) }
            reminders.forEach { carReminderDao.upsert(it) }
            notes.forEach { carNoteDao.upsert(it) }
        }

        return preview.copy(cars = cars)
    }

    suspend fun exportAll(): Map<String, String> {
        val cars = carDao.getAll()
        val events = carEventDao.getAll()
        val reminders = carReminderDao.getAll()
        val notes = carNoteDao.getAll()
        return mapOf(
            CarnotesDtos.GARAGE_TABLE to CarnotesExporter.exportGarage(cars),
            CarnotesDtos.CAR_EVENTS_TABLE to CarnotesExporter.exportEvents(events),
            CarnotesDtos.CAR_REMINDERS_TABLE to CarnotesExporter.exportReminders(reminders),
            CarnotesDtos.NOTES_TABLE to CarnotesExporter.exportNotes(notes),
        )
    }
}
