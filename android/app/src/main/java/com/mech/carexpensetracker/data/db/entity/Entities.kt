package com.mech.carexpensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cars",
    indices = [Index(value = ["externalId"], unique = true)],
)
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val name: String,
    val plateNumber: String? = null,
    val vehicleUnits: String = "km",
    val buyDateMillis: Long? = null,
    val iconColorName: String = "blue",
    val primaryFuelTypeRaw: String = "gasoline",
    val alternativeFuelTypeRaw: String? = null,
)

@Entity(
    tableName = "car_events",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["carExternalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("carExternalId"), Index("externalId", unique = true)],
)
data class CarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val carExternalId: String,
    val typeRaw: String,
    val dateMillis: Long,
    val mileage: Int? = null,
    val comment: String? = null,
    val totalCost: String? = null,
    val categoryName: String? = null,
    val partsCost: String? = null,
    val labourCost: String? = null,
    val fuelTypeRaw: String? = null,
    val fuelAmount: String? = null,
    val fuelCost: String? = null,
    val fuelFullTank: Boolean = false,
    val secondaryFuelTypeRaw: String? = null,
    val secondaryFuelAmount: String? = null,
    val secondaryFuelCost: String? = null,
    val secondaryFuelFullTank: Boolean = false,
)

@Entity(
    tableName = "car_reminders",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["carExternalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("carExternalId"), Index("externalId", unique = true)],
)
data class CarReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val carExternalId: String,
    val title: String,
    val dueDateMillis: Long? = null,
    val dueMileage: Int? = null,
    val isCompleted: Boolean = false,
    val syncedItemIdentifier: String? = null,
    val isObligatory: Boolean = false,
)

@Entity(
    tableName = "car_notes",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["carExternalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("carExternalId"), Index("externalId", unique = true)],
)
data class CarNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val carExternalId: String,
    val title: String,
    val details: String? = null,
    val priorityRaw: String = "normal",
    val isResolved: Boolean = false,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long? = null,
)

@Entity(
    tableName = "expense_categories",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["carExternalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("carExternalId"), Index("externalId", unique = true)],
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val carExternalId: String,
    val name: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "planned_expenses",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["carExternalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("carExternalId"), Index("externalId", unique = true)],
)
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val carExternalId: String,
    val name: String,
    val cost: String,
    val location: String? = null,
    val months: Int? = null,
    val targetDateMillis: Long? = null,
    val horizonRaw: String = "medium",
    val createdAtMillis: Long,
)

@Entity(
    tableName = "event_photos",
    foreignKeys = [
        ForeignKey(
            entity = CarEventEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["eventExternalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventExternalId"), Index("externalId", unique = true)],
)
data class EventPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val eventExternalId: String,
    val imagePath: String,
    val createdAtMillis: Long,
)
