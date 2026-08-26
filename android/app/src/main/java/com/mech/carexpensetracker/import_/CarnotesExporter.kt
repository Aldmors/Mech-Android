package com.mech.carexpensetracker.import_

import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

object CarnotesExporter {
    fun exportGarage(cars: List<CarEntity>): String {
        return buildJsonArray {
            cars.forEach { car ->
                add(
                    buildJsonObject {
                        put("_id", JsonPrimitive(car.externalId))
                        put("name", JsonPrimitive(car.name))
                        car.plateNumber?.let { put("plate_number", JsonPrimitive(it)) }
                        put("vehicle_units", JsonPrimitive(car.vehicleUnits))
                        car.buyDateMillis?.let { put("buy_date", JsonPrimitive(it.toString())) }
                        put("icon_color", JsonPrimitive(car.iconColorName))
                        put("primary_fuel_type", JsonPrimitive(car.primaryFuelTypeRaw))
                        car.alternativeFuelTypeRaw?.let { put("alternative_fuel_type", JsonPrimitive(it)) }
                    },
                )
            }
        }.toString()
    }

    fun exportEvents(events: List<CarEventEntity>): String {
        return buildJsonArray {
            events.forEach { event ->
                add(
                    buildJsonObject {
                        put("_id", JsonPrimitive(event.externalId))
                        put("car_id", JsonPrimitive(event.carExternalId))
                        put("type", JsonPrimitive(event.typeRaw))
                        put("date", JsonPrimitive(event.dateMillis.toString()))
                        event.mileage?.let { put("mileage", JsonPrimitive(it.toString())) }
                        event.comment?.let { put("comment", JsonPrimitive(it)) }
                        event.totalCost?.let { put("total_cost", JsonPrimitive(it)) }
                        event.categoryName?.let { put("category", JsonPrimitive(it)) }
                        event.partsCost?.let { put("parts_cost", JsonPrimitive(it)) }
                        event.labourCost?.let { put("labour_cost", JsonPrimitive(it)) }
                        event.fuelTypeRaw?.let { put("fuel_type", JsonPrimitive(it)) }
                        event.fuelAmount?.let { put("fuel_amount", JsonPrimitive(it)) }
                        event.fuelCost?.let { put("fuel_cost", JsonPrimitive(it)) }
                        put("fuel_full_tank", JsonPrimitive(if (event.fuelFullTank) "1" else "0"))
                        event.secondaryFuelTypeRaw?.let { put("secondary_fuel_type", JsonPrimitive(it)) }
                        event.secondaryFuelAmount?.let { put("secondary_fuel_amount", JsonPrimitive(it)) }
                        event.secondaryFuelCost?.let { put("secondary_fuel_cost", JsonPrimitive(it)) }
                        put(
                            "secondary_fuel_full_tank",
                            JsonPrimitive(if (event.secondaryFuelFullTank) "1" else "0"),
                        )
                    },
                )
            }
        }.toString()
    }

    fun exportReminders(reminders: List<CarReminderEntity>): String {
        return buildJsonArray {
            reminders.forEach { reminder ->
                add(
                    buildJsonObject {
                        put("_id", JsonPrimitive(reminder.externalId))
                        put("car_id", JsonPrimitive(reminder.carExternalId))
                        put("title", JsonPrimitive(reminder.title))
                        reminder.dueDateMillis?.let { put("due_date", JsonPrimitive(it.toString())) }
                        reminder.dueMileage?.let { put("due_mileage", JsonPrimitive(it.toString())) }
                        put("is_completed", JsonPrimitive(if (reminder.isCompleted) "1" else "0"))
                        reminder.syncedItemIdentifier?.let {
                            put("synced_item_identifier", JsonPrimitive(it))
                        }
                    },
                )
            }
        }.toString()
    }

    fun exportNotes(notes: List<CarNoteEntity>): String {
        return buildJsonArray {
            notes.forEach { note ->
                add(
                    buildJsonObject {
                        put("_id", JsonPrimitive(note.externalId))
                        put("car_id", JsonPrimitive(note.carExternalId))
                        put("title", JsonPrimitive(note.title))
                        note.details?.let { put("details", JsonPrimitive(it)) }
                        put("priority", JsonPrimitive(note.priorityRaw))
                        put("is_resolved", JsonPrimitive(if (note.isResolved) "1" else "0"))
                        put("created_at", JsonPrimitive(note.createdAtMillis.toString()))
                        note.resolvedAtMillis?.let { put("resolved_at", JsonPrimitive(it.toString())) }
                    },
                )
            }
        }.toString()
    }
}
