package com.mech.carexpensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Transaction
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
    version = 1,
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
        events.forEach { carEventDao().upsert(it) }
        reminders.forEach { carReminderDao().upsert(it) }
        notes.forEach { carNoteDao().upsert(it) }
    }
}
