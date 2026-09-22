package com.mech.carexpensetracker.import_

import androidx.room.withTransaction
import com.mech.carexpensetracker.data.EventPhotoFiles
import com.mech.carexpensetracker.data.EventPhotoStore
import com.mech.carexpensetracker.data.db.AppDatabase
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
import com.mech.carexpensetracker.domain.service.ObligatoryReminderService

data class ImportPreview(
    val carCount: Int,
    val eventCount: Int,
    val reminderCount: Int,
    val noteCount: Int,
    val photoCount: Int = 0,
    val cars: List<CarEntity>,
)

private data class ParsedCarnotes(
    val cars: List<CarEntity>,
    val events: List<CarEventEntity>,
    val reminders: List<CarReminderEntity>,
    val notes: List<CarNoteEntity>,
    val photos: List<EventPhotoEntity>,
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
    private val eventPhotoDao: EventPhotoDao,
    private val photoStore: EventPhotoStore,
) {
    fun preview(files: Map<String, String>): ImportPreview {
        val parsed = parseFiles(files)
        return ImportPreview(
            carCount = parsed.cars.size,
            eventCount = parsed.events.size,
            reminderCount = parsed.reminders.size,
            noteCount = parsed.notes.size,
            photoCount = parsed.photos.size,
            cars = parsed.cars,
        )
    }

    suspend fun import(
        files: Map<String, String>,
        mode: ImportMode = ImportMode.Merge,
        carOverrides: Map<String, CarEntity> = emptyMap(),
        binaries: Map<String, ByteArray> = emptyMap(),
    ): ImportPreview {
        val parsed = parseFiles(files)
        val events = parsed.events
        val parsedCars = parsed.cars.map { car ->
            carOverrides[car.externalId] ?: car
        }
        val cars = inferFuelTypes(parsedCars, events)
        val reminders = parsed.reminders
        val notes = parsed.notes
        val photos = parsed.photos
        val preview = ImportPreview(
            carCount = cars.size,
            eventCount = events.size,
            reminderCount = reminders.size,
            noteCount = notes.size,
            photoCount = photos.size,
            cars = cars,
        )

        require(cars.isNotEmpty() || events.isEmpty()) {
            "Missing garage_table.json: found ${events.size} events but no cars."
        }

        database.withTransaction {
            val mergedCars = cars.map { imported ->
                val existing = carDao.getByExternalId(imported.externalId)
                if (existing == null) {
                    imported
                } else {
                    imported.copy(
                        primaryFuelTypeRaw = existing.primaryFuelTypeRaw,
                        alternativeFuelTypeRaw = existing.alternativeFuelTypeRaw,
                    )
                }
            }
            mergedCars.forEach { car ->
                val existing = carDao.getByExternalId(car.externalId)
                carDao.upsert(car)
                if (existing == null) {
                    ObligatoryReminderService.seedForCar(car.externalId).forEach { carReminderDao.upsert(it) }
                }
            }

            if (mode == ImportMode.ReplacePerCar) {
                mergedCars.forEach { car ->
                    deletePhotoFilesForCar(car.externalId)
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

            upsertPhotos(photos, binaries)
        }
        return preview.copy(cars = cars)
    }

    private fun parseFiles(files: Map<String, String>): ParsedCarnotes {
        fun rows(key: String) = files[key]?.let { CarnotesParser.parseTable(it) } ?: emptyList()
        return ParsedCarnotes(
            cars = CarnotesParser.parseGarage(rows(CarnotesDtos.GARAGE_TABLE)),
            events = CarnotesParser.parseEvents(rows(CarnotesDtos.CAR_EVENTS_TABLE)),
            reminders = CarnotesParser.parseReminders(rows(CarnotesDtos.CAR_REMINDERS_TABLE)),
            notes = CarnotesParser.parseNotes(rows(CarnotesDtos.NOTES_TABLE)),
            photos = CarnotesParser.parsePhotos(rows(CarnotesDtos.EVENT_PHOTOS_TABLE)),
        )
    }

    private fun inferFuelTypes(
        cars: List<CarEntity>,
        events: List<CarEventEntity>,
    ): List<CarEntity> {
        return cars.map { car ->
            val carEvents = events.filter { it.carExternalId == car.externalId }
            val primaryCounts = carEvents.mapNotNull { it.fuelTypeRaw?.lowercase()?.takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
            val secondaryCounts = carEvents.mapNotNull {
                it.secondaryFuelTypeRaw?.lowercase()?.takeIf(String::isNotBlank)
            }.groupingBy { it }.eachCount()
            val primary = primaryCounts.maxByOrNull { it.value }?.key
            val secondary = secondaryCounts.maxByOrNull { it.value }?.key
                ?: primaryCounts.filterKeys { it != (primary ?: car.primaryFuelTypeRaw) }
                    .maxByOrNull { it.value }?.key
            car.copy(
                primaryFuelTypeRaw = primary ?: car.primaryFuelTypeRaw,
                alternativeFuelTypeRaw = secondary ?: car.alternativeFuelTypeRaw,
            )
        }
    }

    private suspend fun deletePhotoFilesForCar(carExternalId: String) {
        val eventIds = carEventDao.getForCar(carExternalId).map { it.externalId }.toSet()
        eventPhotoDao.getAll().forEach { photo ->
            if (photo.eventExternalId in eventIds) photoStore.delete(photo.imagePath)
        }
    }

    private suspend fun upsertPhotos(
        photos: List<EventPhotoEntity>,
        binaries: Map<String, ByteArray>,
    ) {
        photos.forEach { photo ->
            if (carEventDao.getByExternalId(photo.eventExternalId) == null) return@forEach
            val bytes = EventPhotoFiles.findBinary(binaries, photo.imagePath, photo.externalId)
                ?: return@forEach
            val path = photoStore.save(
                photo.externalId,
                bytes,
                EventPhotoFiles.extensionOf(photo.imagePath, null),
            )
            eventPhotoDao.upsert(photo.copy(id = 0, imagePath = path))
        }
    }

    suspend fun exportAll(): CarnotesZip {
        val cars = carDao.getAll()
        val events = carEventDao.getAll()
        val reminders = carReminderDao.getAll()
        val notes = carNoteDao.getAll()
        val photos = eventPhotoDao.getAll()
        val binaries = linkedMapOf<String, ByteArray>()
        photos.forEach { photo ->
            val bytes = photoStore.readBytes(photo.imagePath) ?: return@forEach
            binaries[EventPhotoFiles.zipEntryName(photo.imagePath, photo.externalId)] = bytes
        }
        return CarnotesZip(
            tables = mapOf(
                CarnotesDtos.GARAGE_TABLE to CarnotesExporter.exportGarage(cars),
                CarnotesDtos.CAR_EVENTS_TABLE to CarnotesExporter.exportEvents(events),
                CarnotesDtos.CAR_REMINDERS_TABLE to CarnotesExporter.exportReminders(reminders),
                CarnotesDtos.NOTES_TABLE to CarnotesExporter.exportNotes(notes),
                CarnotesDtos.EVENT_PHOTOS_TABLE to CarnotesExporter.exportPhotos(photos),
            ),
            binaries = binaries,
        )
    }
}
