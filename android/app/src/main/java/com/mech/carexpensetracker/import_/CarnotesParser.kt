package com.mech.carexpensetracker.import_

import com.mech.carexpensetracker.domain.model.CarIcon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
    const val EXPENSE_CATEGORIES_TABLE = "expense_categories_table.json"
    const val EVENT_PHOTOS_TABLE = "event_photos_table.json"

    val ALL_TABLES = listOf(
        GARAGE_TABLE,
        CAR_EVENTS_TABLE,
        CAR_REMINDERS_TABLE,
        NOTES_TABLE,
        EXPENSE_CATEGORIES_TABLE,
        EVENT_PHOTOS_TABLE,
    )

    fun resolveTableKey(fileName: String?, json: String): String? {
        normalizeFileName(fileName)?.let { name ->
            ALL_TABLES.find { it.equals(name, ignoreCase = true) }?.let { return it }
        }
        val rows = try {
            CarnotesParser.parseTable(json)
        } catch (_: Exception) {
            return null
        }
        if (rows.isEmpty()) return null
        val keys = rows.first().keys
        return when {
            keys.contains("vehicle_units") || (keys.contains("vehicle_type") && !keys.contains("car_id")) -> GARAGE_TABLE
            keys.contains("reminder_date") ||
                (keys.contains("sub_type") && !keys.contains("type") && keys.contains("car_id")) -> CAR_REMINDERS_TABLE
            keys.contains("car_id") && (keys.contains("type") || keys.contains("fuel_volume") || keys.contains("total_cost")) ->
                CAR_EVENTS_TABLE
            keys.contains("color_hex") ||
                (keys.contains("car_id") && keys.contains("icon_name") && keys.contains("name") &&
                    !keys.contains("type") && !keys.contains("title")) ->
                EXPENSE_CATEGORIES_TABLE
            keys.contains("event_id") && (keys.contains("file") || keys.contains("image_path")) ->
                EVENT_PHOTOS_TABLE
            keys.contains("car_id") && (keys.contains("details") || keys.contains("priority")) -> NOTES_TABLE
            else -> null
        }
    }

    internal fun normalizeFileName(fileName: String?): String? {
        val base = fileName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.substringBefore('?')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return COPY_SUFFIX.replace(base, "$2")
    }

    private val COPY_SUFFIX = Regex(""" \((\d+)\)(\.[^.]+)$""")
}

object CarnotesValueParsers {
    fun stringValue(element: JsonElement?): String? {
        if (element == null || element is JsonNull) return null
        return when (element) {
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    fun parseDecimal(raw: String?): java.math.BigDecimal? {
        return com.mech.carexpensetracker.domain.service.CurrencyFormatter.parseStored(raw)
    }

    fun storedDecimal(row: Map<String, String?>, keys: List<String>, zeroAsNull: Boolean = false): String? {
        val value = parseDecimal(field(row, keys)) ?: return null
        if (zeroAsNull && value.signum() == 0) return null
        return value.toPlainString()
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
        val element = kotlinx.serialization.json.Json.parseToJsonElement(
            json.trim().removePrefix("\uFEFF"),
        )
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
                iconName = CarIcon.resolve(
                    CarnotesValueParsers.field(row, listOf("icon_name", "iconName")),
                ),
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
            val typeRaw = CarnotesValueParsers.field(row, listOf("type", "event_type", "typeRaw")) ?: "repair"
            val totalCost = CarnotesValueParsers.storedDecimal(row, listOf("total_cost", "cost", "totalCost"))
            val dateMillis = CarnotesValueParsers.parseLongMillis(
                CarnotesValueParsers.field(row, listOf("date", "event_date", "dateMillis")),
            ) ?: System.currentTimeMillis()
            val importedName = CarnotesValueParsers.field(row, listOf("name"))
            val importedCategory = CarnotesValueParsers.field(row, listOf("category", "category_name"))
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = externalId,
                carExternalId = carId,
                typeRaw = typeRaw,
                dateMillis = dateMillis,
                mileage = CarnotesValueParsers.parseInt(CarnotesValueParsers.field(row, listOf("mileage", "odometer"))),
                comment = CarnotesValueParsers.field(row, listOf("comment", "notes")),
                totalCost = totalCost,
                name = importedName,
                categoryName = importedCategory ?: importedName,
                partsCost = CarnotesValueParsers.storedDecimal(row, listOf("parts_cost", "partsCost")),
                labourCost = CarnotesValueParsers.storedDecimal(row, listOf("labour_cost", "labor_cost", "labourCost")),
                fuelTypeRaw = normalizeFuelType(
                    CarnotesValueParsers.field(row, listOf("fuel_type", "primary_fuel_type")),
                ),
                fuelAmount = CarnotesValueParsers.storedDecimal(
                    row,
                    listOf("fuel_amount", "fuelAmount", "fuel_volume"),
                    zeroAsNull = true,
                ),
                fuelCost = CarnotesValueParsers.storedDecimal(
                    row,
                    listOf("fuel_cost", "fuelCost"),
                    zeroAsNull = true,
                ) ?: totalCost.takeIf { typeRaw.equals("fuel", ignoreCase = true) },
                fuelFullTank = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("fuel_full_tank", "fuelFullTank")),
                ),
                secondaryFuelTypeRaw = normalizeFuelType(
                    CarnotesValueParsers.field(row, listOf("secondary_fuel_type", "alt_fuel_type")),
                ),
                secondaryFuelAmount = CarnotesValueParsers.storedDecimal(
                    row,
                    listOf("secondary_fuel_amount"),
                    zeroAsNull = true,
                ),
                secondaryFuelCost = CarnotesValueParsers.storedDecimal(
                    row,
                    listOf("secondary_fuel_cost"),
                    zeroAsNull = true,
                ),
                secondaryFuelFullTank = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("secondary_fuel_full_tank")),
                ),
            )
        }
    }

    private fun normalizeFuelType(raw: String?): String? = when (raw?.trim()?.lowercase()) {
        null, "" -> null
        "benzyna", "petrol" -> "gasoline"
        else -> raw.trim().lowercase()
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
                    CarnotesValueParsers.field(row, listOf("due_date", "date", "reminder_date")),
                ),
                dueMileage = CarnotesValueParsers.parseInt(
                    CarnotesValueParsers.field(row, listOf("due_mileage", "mileage", "reminder_mileage")),
                ),
                isCompleted = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("is_completed", "completed")),
                ),
                syncedItemIdentifier = CarnotesValueParsers.field(row, listOf("synced_item_identifier", "calendar_id")),
                isObligatory = CarnotesValueParsers.parseBoolean(
                    CarnotesValueParsers.field(row, listOf("is_obligatory", "obligatory")),
                ),
                colorHex = CarnotesValueParsers.field(row, listOf("color_hex", "color"))
                    ?.takeIf { it.startsWith("#") } ?: "#673AB7",
                createdAtMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("created_at", "createdAtMillis")),
                ) ?: 0L,
                intervalDays = CarnotesValueParsers.parseInt(
                    CarnotesValueParsers.field(row, listOf("interval_days", "intervalDays")),
                ),
                intervalKm = CarnotesValueParsers.parseInt(
                    CarnotesValueParsers.field(row, listOf("interval_km", "intervalKm")),
                ),
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

    fun parseCategories(
        rows: List<Map<String, String?>>,
    ): List<com.mech.carexpensetracker.data.db.entity.ExpenseCategoryEntity> {
        return rows.mapNotNull { row ->
            val externalId = CarnotesValueParsers.field(row, listOf("_id", "id"))
            val carId = CarnotesValueParsers.field(row, listOf("car_id", "garage_id"))
            val name = CarnotesValueParsers.field(row, listOf("name", "category_name"))
            if (externalId.isNullOrBlank() || carId.isNullOrBlank() || name.isNullOrBlank()) {
                return@mapNotNull null
            }
            com.mech.carexpensetracker.data.db.entity.ExpenseCategoryEntity(
                externalId = externalId,
                carExternalId = carId,
                name = name,
                createdAtMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("created_at", "createdAt")),
                ) ?: System.currentTimeMillis(),
                colorHex = CarnotesValueParsers.field(row, listOf("color_hex", "color"))
                    ?: com.mech.carexpensetracker.domain.service.CategoryCatalog.DEFAULT_COLOR,
                iconName = CarnotesValueParsers.field(row, listOf("icon_name", "icon")) ?: "Category",
            )
        }
    }

    fun parsePhotos(
        rows: List<Map<String, String?>>,
    ): List<com.mech.carexpensetracker.data.db.entity.EventPhotoEntity> {
        return rows.mapNotNull { row ->
            val externalId = CarnotesValueParsers.field(row, listOf("_id", "id"))
            val eventId = CarnotesValueParsers.field(row, listOf("event_id", "eventExternalId", "eventId"))
            val file = CarnotesValueParsers.field(row, listOf("file", "image_path", "imagePath", "path"))
            if (externalId.isNullOrBlank() || eventId.isNullOrBlank() || file.isNullOrBlank()) {
                return@mapNotNull null
            }
            com.mech.carexpensetracker.data.db.entity.EventPhotoEntity(
                externalId = externalId,
                eventExternalId = eventId,
                imagePath = file,
                createdAtMillis = CarnotesValueParsers.parseLongMillis(
                    CarnotesValueParsers.field(row, listOf("created_at", "createdAt")),
                ) ?: 0L,
            )
        }
    }
}
