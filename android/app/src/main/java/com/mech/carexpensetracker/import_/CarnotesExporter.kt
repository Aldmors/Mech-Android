package com.mech.carexpensetracker.import_

import com.mech.carexpensetracker.data.EventPhotoFiles
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
                        put("icon_name", JsonPrimitive(car.iconName))
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
                        event.name?.let { put("name", JsonPrimitive(it)) }
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
                        put("is_obligatory", JsonPrimitive(if (reminder.isObligatory) "1" else "0"))
                        put("color_hex", JsonPrimitive(reminder.colorHex))
                        put("created_at", JsonPrimitive(reminder.createdAtMillis.toString()))
                        reminder.intervalDays?.let { put("interval_days", JsonPrimitive(it.toString())) }
                        reminder.intervalKm?.let { put("interval_km", JsonPrimitive(it.toString())) }
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

    fun exportPhotos(photos: List<EventPhotoEntity>): String {
        return buildJsonArray {
            photos.forEach { photo ->
                add(
                    buildJsonObject {
                        put("_id", JsonPrimitive(photo.externalId))
                        put("event_id", JsonPrimitive(photo.eventExternalId))
                        put("file", JsonPrimitive(EventPhotoFiles.zipEntryName(photo.imagePath, photo.externalId)))
                        put("created_at", JsonPrimitive(photo.createdAtMillis.toString()))
                    },
                )
            }
        }.toString()
    }

    fun toZip(
        tables: Map<String, String>,
        binaries: Map<String, ByteArray> = emptyMap(),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            tables.forEach { (name, json) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(json.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            binaries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
