package com.mech.carexpensetracker

import android.app.Application
import com.mech.carexpensetracker.reminders.ReminderAlertMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CarExpenseTrackerApp : Application() {
    @Inject lateinit var reminderAlertMonitor: ReminderAlertMonitor

    override fun onCreate() {
        super.onCreate()
        reminderAlertMonitor.start()
    }
}
