package com.mech.carexpensetracker.import_

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

typealias CarnotesRow = Map<String, String?>

object CarnotesDtos {
    const val GARAGE_TABLE = "garage_table.json"
    const val CAR_EVENTS_TABLE = "car_events_table.json"
    const val CAR_REMINDERS_TABLE = "car_reminders_table.json"
    const val NOTES_TABLE = "notes_table.json"

    val ALL_TABLES = listOf(GARAGE_TABLE, CAR_EVENTS_TABLE, CAR_REMINDERS_TABLE, NOTES_TABLE)
}

object CarnotesValueParsers {
    fun stringValue(element: JsonElement?): String? {
        if (element == null) return null
        return when (element) {
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    fun parseDecimal(raw: String?): java.math.BigDecimal? {
        if (raw.isNullOrBlank() || raw == "-1") return null
        return raw.replace(",", ".").toBigDecimalOrNull()
    }

    fun parseInt(raw: String?): Int? {
        if (raw.isNullOrBlank() || raw == "-1") return null
        return raw.toIntOrNull()
    }

    fun parseLongMillis(raw: String?): Long? {
        if (raw.isNullOrBlank() || raw == "-1") return null
        return raw.toLongOrNull()
    }

    fun parseBoolean(raw: String?): Boolean {
        return raw == "1" || raw.equals("true", ignoreCase = true)
    }

    fun rowsFromJson(element: JsonElement): List<Map<String, String?>> {
        return when (element) {
            is JsonArray -> element.map { row ->
                row.jsonObject.mapValues { (_, v) -> stringValue(v) }
            }
            is JsonObject -> {
                when {
                    element.containsKey("rows") -> {
                        element["rows"]!!.jsonArray.map { row ->
                            row.jsonObject.mapValues { (_, v) -> stringValue(v) }
                        }
                    }
                    element.containsKey("data") -> {
                        element["data"]!!.jsonArray.map { row ->
                            row.jsonObject.mapValues { (_, v) -> stringValue(v) }
                        }
                    }
                    else -> listOf(element.mapValues { (_, v) -> stringValue(v) })
                }
            }
            else -> emptyList()
        }
    }

    fun field(row: Map<String, String?>, keys: List<String>): String? {
        for (key in keys) {
            val value = row[key]
            if (!value.isNullOrBlank()) return value
        }
        return null
    }
}

object CarnotesParser {
    fun parseTable(json: String): List<Map<String, String?>> {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(json)
        return CarnotesValueParsers.rowsFromJson(element)
    }

    fun parseGarage(rows: List<Map<String, String?>>): List<com.mech.carexpensetracker.data.db.entity.CarEntity> {
        return rows.mapNotNull { row ->
            val externalId = CarnotesValueParsers.field(row, listOf("_id", "id", "externalId"))
            if (externalId.isNullOrBlank()) return@mapNotNull null
            com.mech.carexpensetracker.data.db.entity.CarEntity(
                externalId = externalId,
                name = CarnotesValueParsers.field(row, listOf("name", "car_name")) ?: "Car",
                plateNumber = CarnotesValueParsers.field(row, listOf("plate_number", "plateNumber")),
                vehicleUnits = CarnotesValueParsers.field(row, listOf("vehicle_units", "units")) ?: "km",
                buyDateMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("buy_date", "buyDate")),
                ),
                iconColorName = CarnotesValueParsers.field(row, listOf("icon_color", "iconColorName")) ?: "blue",
                primaryFuelTypeRaw = CarnotesValueParsers.field(row, listOf("primary_fuel_type", "fuel_type")) ?: "gasoline",
                alternativeFuelTypeRaw = CarnotesValueParsers.field(row, listOf("alternative_fuel_type", "alt_fuel_type")),
            )
        }
    }

    fun parseEvents(rows: List<Map<String, String?>>): List<com.mech.carexpensetracker.data.db.entity.CarEventEntity> {
        return rows.mapNotNull { row ->
            val externalId = CarnotesValueParsers.field(row, listOf("_id", "id"))
            val carId = CarnotesValueParsers.field(row, listOf("car_id", "garage_id", "carExternalId"))
            if (externalId.isNullOrBlank() || carId.isNullOrBlank()) return@mapNotNull null
            val dateMillis = CarnotesValueParsers.parseLongMillis(
                CarnotesValueParsers.field(row, listOf("date", "event_date", "dateMillis")),
            ) ?: System.currentTimeMillis()
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = externalId,
                carExternalId = carId,
                typeRaw = CarnotesValueParsers.field(row, listOf("type", "event_type", "typeRaw")) ?: "repair",
                dateMillis = dateMillis,
                mileage = CarnotesValueParsers.parseInt(CarnotesValueParsers.field(row, listOf("mileage", "odometer"))),
                comment = CarnotesValueParsers.field(row, listOf("comment", "notes")),
                totalCost = CarnotesValueParsers.field(row, listOf("total_cost", "cost", "totalCost")),
                categoryName = CarnotesValueParsers.field(row, listOf("category", "category_name")),
                partsCost = CarnotesValueParsers.field(row, listOf("parts_cost", "partsCost")),
                labourCost = CarnotesValueParsers.field(row, listOf("labour_cost", "labor_cost", "labourCost")),
                fuelTypeRaw = CarnotesValueParsers.field(row, listOf("fuel_type", "primary_fuel_type")),
                fuelAmount = CarnotesValueParsers.field(row, listOf("fuel_amount", "fuelAmount")),
                fuelCost = CarnotesValueParsers.field(row, listOf("fuel_cost", "fuelCost")),
                fuelFullTank = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("fuel_full_tank", "fuelFullTank")),
                ),
                secondaryFuelTypeRaw = CarnotesValueParsers.field(row, listOf("secondary_fuel_type", "alt_fuel_type")),
                secondaryFuelAmount = CarnotesValueParsers.field(row, listOf("secondary_fuel_amount")),
                secondaryFuelCost = CarnotesValueParsers.field(row, listOf("secondary_fuel_cost")),
                secondaryFuelFullTank = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("secondary_fuel_full_tank")),
                ),
            )
        }
    }

    fun parseReminders(rows: List<Map<String, String?>>): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        return rows.mapNotNull { row ->
            val externalId = CarnotesValueParsers.field(row, listOf("_id", "id"))
            val carId = CarnotesValueParsers.field(row, listOf("car_id", "garage_id"))
            if (externalId.isNullOrBlank() || carId.isNullOrBlank()) return@mapNotNull null
            com.mech.carexpensetracker.data.db.entity.CarReminderEntity(
                externalId = externalId,
                carExternalId = carId,
                title = CarnotesValueParsers.field(row, listOf("title", "name")) ?: "Reminder",
                dueDateMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("due_date", "date")),
                ),
                dueMileage = CarnotesValueParsers.parseInt(
                    CarnotesValueParsers.field(row, listOf("due_mileage", "mileage")),
                ),
                isCompleted = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("is_completed", "completed")),
                ),
                syncedItemIdentifier = CarnotesValueParsers.field(row, listOf("synced_item_identifier", "calendar_id")),
            )
        }
    }

    fun parseNotes(rows: List<Map<String, String?>>): List<com.mech.carexpensetracker.data.db.entity.CarNoteEntity> {
        return rows.mapNotNull { row ->
            val externalId = CarnotesValueParsers.field(row, listOf("_id", "id"))
            val carId = CarnotesValueParsers.field(row, listOf("car_id", "garage_id"))
            if (externalId.isNullOrBlank() || carId.isNullOrBlank()) return@mapNotNull null
            com.mech.carexpensetracker.data.db.entity.CarNoteEntity(
                externalId = externalId,
                carExternalId = carId,
                title = CarnotesValueParsers.field(row, listOf("title", "name")) ?: "Note",
                details = CarnotesValueParsers.field(row, listOf("details", "body")),
                priorityRaw = CarnotesValueParsers.field(row, listOf("priority")) ?: "normal",
                isResolved = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("is_resolved", "resolved")),
                ),
                createdAtMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("created_at", "createdAt")),
                ) ?: System.currentTimeMillis(),
                resolvedAtMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("resolved_at", "resolvedAt")),
                ),
            )
        }
    }
}
