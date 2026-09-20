package com.mech.carexpensetracker.import_

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CarnotesParserTest {
    private fun readFixture(name: String): String {
        val path = "carnotes_zip_exported_1781131397635/$name"
        return javaClass.classLoader?.getResourceAsStream(path)?.bufferedReader()?.readText()
            ?: error("Missing fixture: $path")
    }

    @Test
    fun parseSampleGarage() {
        val rows = CarnotesParser.parseTable(readFixture("garage_table.json"))
        val cars = CarnotesParser.parseGarage(rows)
        assertTrue(cars.size >= 1)
        assertEquals("DS", cars.first().name)
        assertEquals("km", cars.first().vehicleUnits)
    }

    @Test
    fun parseSampleEvents() {
        val rows = CarnotesParser.parseTable(readFixture("car_events_table.json"))
        val events = CarnotesParser.parseEvents(rows)
        assertTrue(events.size >= 50)
        val event21 = events.find { it.externalId == "21" }
        assertTrue(event21 != null)
        assertEquals("lpg", event21?.secondaryFuelTypeRaw)
    }

    @Test
    fun sentinelMinusOneBecomesNull() {
        assertEquals(null, CarnotesValueParsers.parseInt("-1"))
    }

    @Test
    fun fuelFullTankBoolean() {
        assertTrue(CarnotesValueParsers.parseBoolean("1"))
        assertEquals(false, CarnotesValueParsers.parseBoolean("0"))
    }

    private fun readIosFixture(name: String): String {
        val path = "carnotes_ios_export/$name"
        return javaClass.classLoader?.getResourceAsStream(path)?.bufferedReader()?.readText()
            ?: error("Missing fixture: $path")
    }

    @Test
    fun parseIosExportGarageAndEvents() {
        val garageJson = readIosFixture("garage_table.json")
        val eventsJson = readIosFixture("car_events_table.json")
        val remindersJson = readIosFixture("car_reminders_table.json")

        assertEquals(CarnotesDtos.GARAGE_TABLE, CarnotesDtos.resolveTableKey("garage_table.json", garageJson))
        assertEquals(CarnotesDtos.CAR_EVENTS_TABLE, CarnotesDtos.resolveTableKey("car_events_table.json", eventsJson))
        assertEquals(CarnotesDtos.CAR_REMINDERS_TABLE, CarnotesDtos.resolveTableKey("car_reminders_table.json", remindersJson))

        val cars = CarnotesParser.parseGarage(CarnotesParser.parseTable(garageJson))
        val events = CarnotesParser.parseEvents(CarnotesParser.parseTable(eventsJson))
        val reminders = CarnotesParser.parseReminders(CarnotesParser.parseTable(remindersJson))

        assertEquals(2, cars.size)
        assertEquals(67, events.size)
        assertEquals(7, reminders.size)
        assertEquals("17", events.first { it.externalId == "26" }.fuelAmount)
        assertEquals("5.886", events.first { it.externalId == "26" }.fuelCost)
        assertEquals("diesel", events.first { it.externalId == "26" }.fuelTypeRaw)
    }
}
