package com.mech.carexpensetracker.di

import android.content.Context
import androidx.room.Room
import com.mech.carexpensetracker.data.EventPhotoStore
import com.mech.carexpensetracker.data.db.AppDatabase
import com.mech.carexpensetracker.data.db.CarDao
import com.mech.carexpensetracker.data.db.CarEventDao
import com.mech.carexpensetracker.data.db.CarNoteDao
import com.mech.carexpensetracker.data.db.CarReminderDao
import com.mech.carexpensetracker.data.db.EventPhotoDao
import com.mech.carexpensetracker.data.db.PlannedExpenseDao
import com.mech.carexpensetracker.data.prefs.SelectedCarStore
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventPhotoRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.data.repository.NoteRepository
import com.mech.carexpensetracker.data.repository.ReminderRepository
import com.mech.carexpensetracker.import_.ImportCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "car_expense_tracker.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()

    @Provides fun provideCarDao(db: AppDatabase): CarDao = db.carDao()
    @Provides fun provideCarEventDao(db: AppDatabase): CarEventDao = db.carEventDao()
    @Provides fun provideCarReminderDao(db: AppDatabase): CarReminderDao = db.carReminderDao()
    @Provides fun provideCarNoteDao(db: AppDatabase): CarNoteDao = db.carNoteDao()
    @Provides fun providePlannedExpenseDao(db: AppDatabase): PlannedExpenseDao = db.plannedExpenseDao()
    @Provides fun provideEventPhotoDao(db: AppDatabase): EventPhotoDao = db.eventPhotoDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSelectedCarStore(@ApplicationContext context: Context): SelectedCarStore =
        SelectedCarStore(context)

    @Provides
    @Singleton
    fun provideCarRepository(
        carDao: CarDao,
        carEventDao: CarEventDao,
        carReminderDao: CarReminderDao,
        selectedCarStore: SelectedCarStore,
    ): CarRepository = CarRepository(carDao, carEventDao, carReminderDao, selectedCarStore)

    @Provides
    @Singleton
    fun provideEventRepository(carEventDao: CarEventDao): EventRepository =
        EventRepository(carEventDao)

    @Provides
    @Singleton
    fun provideReminderRepository(carReminderDao: CarReminderDao): ReminderRepository =
        ReminderRepository(carReminderDao)

    @Provides
    @Singleton
    fun provideNoteRepository(carNoteDao: CarNoteDao): NoteRepository =
        NoteRepository(carNoteDao)

    @Provides
    @Singleton
    fun provideEventPhotoStore(@ApplicationContext context: Context): EventPhotoStore =
        EventPhotoStore(context)

    @Provides
    @Singleton
    fun provideEventPhotoRepository(
        eventPhotoDao: EventPhotoDao,
        photoStore: EventPhotoStore,
    ): EventPhotoRepository = EventPhotoRepository(eventPhotoDao, photoStore)

    @Provides
    @Singleton
    fun provideImportCoordinator(
        database: AppDatabase,
        carDao: CarDao,
        carEventDao: CarEventDao,
        carReminderDao: CarReminderDao,
        carNoteDao: CarNoteDao,
        eventPhotoDao: EventPhotoDao,
        photoStore: EventPhotoStore,
    ): ImportCoordinator = ImportCoordinator(
        database,
        carDao,
        carEventDao,
        carReminderDao,
        carNoteDao,
        eventPhotoDao,
        photoStore,
    )
}
