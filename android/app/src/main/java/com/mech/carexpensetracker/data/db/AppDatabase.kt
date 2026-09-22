package com.mech.carexpensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.db.entity.ExpenseCategoryEntity
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import com.mech.carexpensetracker.data.db.entity.PlannedExpenseEntity

@Database(
    entities = [
        CarEntity::class,
        CarEventEntity::class,
        CarReminderEntity::class,
        CarNoteEntity::class,
        ExpenseCategoryEntity::class,
        PlannedExpenseEntity::class,
        EventPhotoEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun carEventDao(): CarEventDao
    abstract fun carReminderDao(): CarReminderDao
    abstract fun carNoteDao(): CarNoteDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun plannedExpenseDao(): PlannedExpenseDao
    abstract fun eventPhotoDao(): EventPhotoDao

    @Transaction
    open suspend fun replaceCarData(
        carExternalId: String,
        events: List<CarEventEntity>,
        reminders: List<CarReminderEntity>,
        notes: List<CarNoteEntity>,
    ) {
        carEventDao().deleteForCar(carExternalId)
        carReminderDao().deleteForCar(carExternalId)
        carNoteDao().deleteForCar(carExternalId)
        events.forEach { carEventDao().upsert(it) }
        reminders.forEach { carReminderDao().upsert(it) }
        notes.forEach { carNoteDao().upsert(it) }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE expense_categories ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#673AB7'",
                )
                db.execSQL(
                    "ALTER TABLE expense_categories ADD COLUMN iconName TEXT NOT NULL DEFAULT 'Category'",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cars ADD COLUMN iconName TEXT NOT NULL DEFAULT 'DirectionsCar'",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE car_events ADD COLUMN name TEXT")
                db.execSQL(
                    "UPDATE car_events SET name = categoryName " +
                        "WHERE name IS NULL AND categoryName IS NOT NULL AND categoryName != ''",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE car_reminders ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#673AB7'",
                )
                db.execSQL(
                    "ALTER TABLE car_reminders ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE car_reminders ADD COLUMN intervalDays INTEGER")
                db.execSQL("ALTER TABLE car_reminders ADD COLUMN intervalKm INTEGER")
            }
        }
    }
}
