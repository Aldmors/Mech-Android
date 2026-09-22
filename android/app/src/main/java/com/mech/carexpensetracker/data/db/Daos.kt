package com.mech.carexpensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarNoteEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.db.entity.ExpenseCategoryEntity
import com.mech.carexpensetracker.data.db.entity.EventPhotoEntity
import com.mech.carexpensetracker.data.db.entity.PlannedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY name")
    fun observeAll(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars ORDER BY name")
    suspend fun getAll(): List<CarEntity>

    @Query("SELECT * FROM cars WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): CarEntity?

    @Query("SELECT * FROM cars WHERE externalId = :externalId")
    fun observeByExternalId(externalId: String): Flow<CarEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(car: CarEntity): Long

    @Update
    suspend fun update(car: CarEntity)

    @Transaction
    suspend fun upsert(car: CarEntity) {
        val existing = getByExternalId(car.externalId)
        if (existing == null) {
            insert(car.copy(id = 0))
        } else {
            update(car.copy(id = existing.id))
        }
    }

    @Query("DELETE FROM cars WHERE externalId = :externalId")
    suspend fun deleteByExternalId(externalId: String)

    @Query("SELECT COUNT(*) FROM cars")
    suspend fun count(): Int
}

@Dao
interface CarEventDao {
    @Query("SELECT * FROM car_events WHERE carExternalId = :carExternalId ORDER BY dateMillis DESC")
    fun observeForCar(carExternalId: String): Flow<List<CarEventEntity>>

    @Query("SELECT * FROM car_events WHERE carExternalId = :carExternalId ORDER BY dateMillis DESC")
    suspend fun getForCar(carExternalId: String): List<CarEventEntity>

    @Query("SELECT * FROM car_events WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): CarEventEntity?

    @Query("SELECT * FROM car_events ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<CarEventEntity>>

    @Query("SELECT * FROM car_events ORDER BY dateMillis DESC")
    suspend fun getAll(): List<CarEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: CarEventEntity): Long

    @Update
    suspend fun update(event: CarEventEntity)

    @Transaction
    suspend fun upsert(event: CarEventEntity) {
        val existing = getByExternalId(event.externalId)
        if (existing == null) {
            insert(event.copy(id = 0))
        } else {
            update(event.copy(id = existing.id))
        }
    }

    @Query("DELETE FROM car_events WHERE externalId = :externalId")
    suspend fun deleteByExternalId(externalId: String)

    @Query("DELETE FROM car_events WHERE carExternalId = :carExternalId")
    suspend fun deleteForCar(carExternalId: String)

    @Query("SELECT COUNT(*) FROM car_events WHERE carExternalId = :carExternalId")
    suspend fun countForCar(carExternalId: String): Int
}

@Dao
interface CarReminderDao {
    @Query("SELECT * FROM car_reminders WHERE carExternalId = :carExternalId ORDER BY dueDateMillis")
    fun observeForCar(carExternalId: String): Flow<List<CarReminderEntity>>

    @Query("SELECT * FROM car_reminders WHERE carExternalId = :carExternalId ORDER BY dueDateMillis")
    suspend fun getForCar(carExternalId: String): List<CarReminderEntity>

    @Query("SELECT * FROM car_reminders ORDER BY dueDateMillis")
    fun observeAll(): Flow<List<CarReminderEntity>>

    @Query("SELECT * FROM car_reminders ORDER BY dueDateMillis")
    suspend fun getAll(): List<CarReminderEntity>

    @Query("SELECT * FROM car_reminders WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): CarReminderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reminder: CarReminderEntity): Long

    @Update
    suspend fun update(reminder: CarReminderEntity)

    @Transaction
    suspend fun upsert(reminder: CarReminderEntity) {
        val existing = getByExternalId(reminder.externalId)
        if (existing == null) {
            insert(reminder.copy(id = 0))
        } else {
            update(reminder.copy(id = existing.id))
        }
    }

    @Query("DELETE FROM car_reminders WHERE carExternalId = :carExternalId")
    suspend fun deleteForCar(carExternalId: String)

    @Query("DELETE FROM car_reminders WHERE externalId = :externalId")
    suspend fun deleteByExternalId(externalId: String)
}

@Dao
interface CarNoteDao {
    @Query("SELECT * FROM car_notes WHERE carExternalId = :carExternalId ORDER BY createdAtMillis DESC")
    fun observeForCar(carExternalId: String): Flow<List<CarNoteEntity>>

    @Query("SELECT * FROM car_notes ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<CarNoteEntity>

    @Query("SELECT * FROM car_notes WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): CarNoteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: CarNoteEntity): Long

    @Update
    suspend fun update(note: CarNoteEntity)

    @Transaction
    suspend fun upsert(note: CarNoteEntity) {
        val existing = getByExternalId(note.externalId)
        if (existing == null) {
            insert(note.copy(id = 0))
        } else {
            update(note.copy(id = existing.id))
        }
    }

    @Query("DELETE FROM car_notes WHERE carExternalId = :carExternalId")
    suspend fun deleteForCar(carExternalId: String)
}

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY name")
    suspend fun getAll(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_categories WHERE carExternalId = :carExternalId ORDER BY name")
    fun observeForCar(carExternalId: String): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): ExpenseCategoryEntity?

    @Query(
        "SELECT * FROM expense_categories WHERE carExternalId = :carExternalId AND LOWER(name) = LOWER(:name) LIMIT 1",
    )
    suspend fun getByNameIgnoreCase(carExternalId: String, name: String): ExpenseCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: ExpenseCategoryEntity): Long

    @Update
    suspend fun update(category: ExpenseCategoryEntity)

    @Transaction
    suspend fun upsert(category: ExpenseCategoryEntity) {
        val existing = getByExternalId(category.externalId)
        if (existing == null) {
            insert(category.copy(id = 0))
        } else {
            update(category.copy(id = existing.id))
        }
    }
}

@Dao
interface PlannedExpenseDao {
    @Query("SELECT * FROM planned_expenses WHERE carExternalId = :carExternalId ORDER BY createdAtMillis DESC")
    fun observeForCar(carExternalId: String): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT * FROM planned_expenses WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): PlannedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(expense: PlannedExpenseEntity): Long

    @Update
    suspend fun update(expense: PlannedExpenseEntity)

    @Transaction
    suspend fun upsert(expense: PlannedExpenseEntity) {
        val existing = getByExternalId(expense.externalId)
        if (existing == null) {
            insert(expense.copy(id = 0))
        } else {
            update(expense.copy(id = existing.id))
        }
    }
}

@Dao
interface EventPhotoDao {
    @Query("SELECT * FROM event_photos ORDER BY createdAtMillis")
    fun observeAll(): Flow<List<EventPhotoEntity>>

    @Query("SELECT * FROM event_photos ORDER BY createdAtMillis")
    suspend fun getAll(): List<EventPhotoEntity>

    @Query(
        "SELECT * FROM event_photos WHERE eventExternalId = :eventExternalId ORDER BY createdAtMillis",
    )
    fun observeForEvent(eventExternalId: String): Flow<List<EventPhotoEntity>>

    @Query(
        "SELECT * FROM event_photos WHERE eventExternalId = :eventExternalId ORDER BY createdAtMillis",
    )
    suspend fun getForEvent(eventExternalId: String): List<EventPhotoEntity>

    @Query("SELECT * FROM event_photos WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): EventPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(photo: EventPhotoEntity): Long

    @Update
    suspend fun update(photo: EventPhotoEntity)

    @Transaction
    suspend fun upsert(photo: EventPhotoEntity) {
        val existing = getByExternalId(photo.externalId)
        if (existing == null) {
            insert(photo.copy(id = 0))
        } else {
            update(photo.copy(id = existing.id))
        }
    }

    @Query("DELETE FROM event_photos WHERE externalId = :externalId")
    suspend fun deleteByExternalId(externalId: String)
}
