package com.mech.carexpensetracker.import_

import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CarnotesExporterTest {
    @Test
    fun zipRoundTripKeepsCanonicalFields() {
        val car = CarEntity(
            externalId = "car-1",
            name = "DS",
            plateNumber = "DS 12345",
            vehicleUnits = "km",
            buyDateMillis = 1704067200000,
            iconColorName = "blue",
            iconName = "ElectricCar",
            primaryFuelTypeRaw = "gasoline",
            alternativeFuelTypeRaw = "lpg",
        )
        val event = CarEventEntity(
            externalId = "ev-1",
            carExternalId = "car-1",
            typeRaw = "fuel",
            dateMillis = 1704672000000,
            mileage = 10250,
            comment = "Full",
            totalCost = "100.07",
            categoryName = "Fuel",
            name = "Tankowanie",
            fuelTypeRaw = "gasoline",
            fuelAmount = "17",
            fuelCost = "100.07",
            fuelFullTank = true,
            secondaryFuelTypeRaw = "lpg",
            secondaryFuelAmount = "20",
            secondaryFuelCost = "40",
            secondaryFuelFullTank = false,
        )
        val reminder = CarReminderEntity(
            externalId = "r-1",
            carExternalId = "car-1",
            title = "Oil change",
            dueDateMillis = 1735603200000,
            dueMileage = 15000,
            isCompleted = false,
            isObligatory = true,
        )
        val note = CarNoteEntity(
            externalId = "n-1",
            carExternalId = "car-1",
            title = "Tires",
            details = "Check pressure",
            priorityRaw = "high",
            isResolved = true,
            createdAtMillis = 1704067200000,
            resolvedAtMillis = 1704153600000,
        )
        val photo = EventPhotoEntity(
            externalId = "p-1",
            eventExternalId = "ev-1",
            imagePath = "event_photos/p-1.jpg",
            createdAtMillis = 1704067200000,
        )
        val photoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02)

        val tables = mapOf(
            CarnotesDtos.GARAGE_TABLE to CarnotesExporter.exportGarage(listOf(car)),
            CarnotesDtos.CAR_EVENTS_TABLE to CarnotesExporter.exportEvents(listOf(event)),
            CarnotesDtos.CAR_REMINDERS_TABLE to CarnotesExporter.exportReminders(listOf(reminder)),
            CarnotesDtos.NOTES_TABLE to CarnotesExporter.exportNotes(listOf(note)),
            CarnotesDtos.EVENT_PHOTOS_TABLE to CarnotesExporter.exportPhotos(listOf(photo)),
        )
        val parsedArchive = ImportFileReader.readZip(
            CarnotesExporter.toZip(tables, mapOf("event_photos/p-1.jpg" to photoBytes)),
        )
        assertEquals(tables.keys, parsedArchive.tables.keys)
        assertTrue(parsedArchive.binaries["event_photos/p-1.jpg"]?.contentEquals(photoBytes) == true)

        val parsedCar = CarnotesParser.parseGarage(
            CarnotesParser.parseTable(parsedArchive.tables.getValue(CarnotesDtos.GARAGE_TABLE)),
        ).single()
        val parsedEvent = CarnotesParser.parseEvents(
            CarnotesParser.parseTable(parsedArchive.tables.getValue(CarnotesDtos.CAR_EVENTS_TABLE)),
        ).single()
        val parsedReminder = CarnotesParser.parseReminders(
            CarnotesParser.parseTable(parsedArchive.tables.getValue(CarnotesDtos.CAR_REMINDERS_TABLE)),
        ).single()
        val parsedNote = CarnotesParser.parseNotes(
            CarnotesParser.parseTable(parsedArchive.tables.getValue(CarnotesDtos.NOTES_TABLE)),
        ).single()
        val parsedPhoto = CarnotesParser.parsePhotos(
            CarnotesParser.parseTable(parsedArchive.tables.getValue(CarnotesDtos.EVENT_PHOTOS_TABLE)),
        ).single()

        assertEquals(car, parsedCar)
        assertEquals(event, parsedEvent)
        assertEquals(reminder, parsedReminder)
        assertEquals(note, parsedNote)
        assertEquals(photo, parsedPhoto)
        assertTrue(parsedReminder.isObligatory)
        assertEquals(
            CarnotesDtos.EVENT_PHOTOS_TABLE,
            CarnotesDtos.resolveTableKey(null, tables.getValue(CarnotesDtos.EVENT_PHOTOS_TABLE)),
        )
    }
}
