package com.mech.carexpensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(car: CarEntity): Long

    @Update
    suspend fun update(car: CarEntity)

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
    suspend fun getAll(): List<CarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CarEventEntity): Long

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

    @Query("SELECT * FROM car_reminders ORDER BY dueDateMillis")
    suspend fun getAll(): List<CarReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: CarReminderEntity): Long

    @Query("DELETE FROM car_reminders WHERE carExternalId = :carExternalId")
    suspend fun deleteForCar(carExternalId: String)
}

@Dao
interface CarNoteDao {
    @Query("SELECT * FROM car_notes WHERE carExternalId = :carExternalId ORDER BY createdAtMillis DESC")
    fun observeForCar(carExternalId: String): Flow<List<CarNoteEntity>>

    @Query("SELECT * FROM car_notes ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<CarNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: CarNoteEntity): Long
}

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories WHERE carExternalId = :carExternalId ORDER BY name")
    fun observeForCar(carExternalId: String): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: ExpenseCategoryEntity): Long
}

@Dao
interface PlannedExpenseDao {
    @Query("SELECT * FROM planned_expenses WHERE carExternalId = :carExternalId ORDER BY createdAtMillis DESC")
    fun observeForCar(carExternalId: String): Flow<List<PlannedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: PlannedExpenseEntity): Long
}

@Dao
interface EventPhotoDao {
    @Query("SELECT * FROM event_photos WHERE eventExternalId = :eventExternalId")
    fun observeForEvent(eventExternalId: String): Flow<List<EventPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: EventPhotoEntity): Long
}
