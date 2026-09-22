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
        assertEquals("DirectionsCar", cars.first().iconName)
    }

    @Test
    fun parseSampleEvents() {
        val rows = CarnotesParser.parseTable(readFixture("car_events_table.json"))
        val events = CarnotesParser.parseEvents(rows)
        assertTrue(events.size >= 50)
        val event21 = events.find { it.externalId == "21" }
        assertTrue(event21 != null)
        assertEquals("lpg", event21?.secondaryFuelTypeRaw)
        assertEquals("Service", events.find { it.externalId == "1" }?.categoryName)
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
        assertEquals("100.07", events.first { it.externalId == "26" }.totalCost)
        assertEquals("100.07", events.first { it.externalId == "26" }.fuelCost)
        assertEquals("diesel", events.first { it.externalId == "26" }.fuelTypeRaw)
        assertEquals("Wycieraczki przód", events.first { it.externalId == "31" }.categoryName)
        assertEquals("Ubezpieczenie OC", events.first { it.externalId == "17" }.name)
        assertEquals("Ubezpieczenie OC", events.first { it.externalId == "17" }.categoryName)
        assertEquals(null, events.first { it.externalId == "12" }.totalCost)
        assertEquals(
            "Wymiana oleju w skrzyni biegów,\nwymiana dwumasy, wymiana wysprzęglika, adaptacja skrzyni, wymiana oleju w robocie ",
            events.first { it.externalId == "22" }.comment,
        )
        assertEquals(
            "gasoline",
            events.first { it.externalId == "B7ECA192-4E74-4763-986C-28E0490464D8" }.fuelTypeRaw,
        )
        assertEquals(
            java.math.BigDecimal("6.44"),
            com.mech.carexpensetracker.domain.service.ConsumptionCalculator
                .consumptionByEventId(events, com.mech.carexpensetracker.domain.model.VehicleUnits.Km)["53"],
        )
    }

    @Test
    fun jsonNullStaysNull() {
        val rows = CarnotesParser.parseTable(
            """[{"_id":"1","car_id":"car","type":"repair","date":"1","comment":null}]""",
        )
        assertEquals(null, CarnotesParser.parseEvents(rows).single().comment)
    }

    @Test
    fun zipAndInvalidJsonDoNotCrash() {
        assertEquals(null, CarnotesDtos.resolveTableKey("export.zip", "not-json"))
        val json = """[{"_id":"1","name":"X","vehicle_units":"km"}]"""
        val zipBytes = java.io.ByteArrayOutputStream().use { out ->
            java.util.zip.ZipOutputStream(out).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("garage_table.json"))
                zip.write(json.toByteArray())
                zip.closeEntry()
            }
            out.toByteArray()
        }
        val tables = ImportFileReader.tablesFromZip(zipBytes)
        val cars = CarnotesParser.parseGarage(CarnotesParser.parseTable(tables.getValue(CarnotesDtos.GARAGE_TABLE)))
        assertEquals(1, cars.size)
        assertEquals("X", cars.first().name)
    }

    @Test
    fun copySuffixAndSeparateJsonFilesResolve() {
        assertEquals(
            CarnotesDtos.GARAGE_TABLE,
            CarnotesDtos.resolveTableKey("garage_table (1).json", "[]"),
        )
        val garage = """[{"_id":"1","name":"X","vehicle_units":"km"}]""".toByteArray()
        val events = """[{"_id":"2","car_id":"1","type":"fuel","total_cost":"10"}]""".toByteArray()
        val reminders = """[{"_id":"3","car_id":"1","name":"Oil","reminder_date":"1"}]""".toByteArray()
        val tables = ImportFileReader.tablesFromNamedContents(
            listOf(
                "garage_table (1).json" to garage,
                "car_events_table.json" to events,
                "reminders.json" to reminders,
            ),
        )
        assertEquals(
            setOf(CarnotesDtos.GARAGE_TABLE, CarnotesDtos.CAR_EVENTS_TABLE, CarnotesDtos.CAR_REMINDERS_TABLE),
            tables.keys,
        )
    }
}
