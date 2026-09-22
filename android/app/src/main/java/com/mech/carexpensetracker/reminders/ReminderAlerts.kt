package com.mech.carexpensetracker.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mech.carexpensetracker.MainActivity
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.data.repository.ReminderRepository
import com.mech.carexpensetracker.domain.service.EventSummaryService
import com.mech.carexpensetracker.domain.service.ReminderAlertService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "reminders"
private const val NOTIFICATION_ID = 1
const val ACTION_REMINDER_ALERT = "com.mech.carexpensetracker.REMINDER_ALERT"

@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun sync(items: List<Pair<CarReminderEntity, String>>) {
        ensureChannel()
        if (!canNotify()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val incoming = items.map { it.first.externalId }.toSet()
        manager.activeNotifications
            .filter { it.notification.channelId == CHANNEL_ID }
            .filter { it.tag !in incoming }
            .forEach { manager.cancel(it.tag, it.id) }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        items.forEach { (reminder, carName) ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(reminder.title)
                .setContentText(carName)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .apply {
                    runCatching { setColor(android.graphics.Color.parseColor(reminder.colorHex)) }
                }
                .build()
            manager.notify(reminder.externalId, NOTIFICATION_ID, notification)
        }
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }
}

@Singleton
class ReminderAlertMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val eventRepository: EventRepository,
    private val carRepository: CarRepository,
    private val notifier: ReminderNotifier,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scheduleDaily()
        if (!started.compareAndSet(false, true)) return
        combine(
            reminderRepository.observeAll(),
            eventRepository.observeAll(),
            carRepository.observeCars(),
        ) { reminders, events, cars ->
            val mileageByCar = events.groupBy { it.carExternalId }
                .mapValues { EventSummaryService.currentMileage(it.value) }
            val names = cars.associate { it.externalId to it.name }
            ReminderAlertService.approachingReminders(reminders, mileageByCar).map { reminder ->
                reminder to (names[reminder.carExternalId].orEmpty())
            }
        }.onEach { notifier.sync(it) }
            .launchIn(scope)
    }

    suspend fun refreshOnce() {
        val reminders = reminderRepository.getAll()
        val events = eventRepository.getAll()
        val cars = carRepository.getCars()
        val mileageByCar = events.groupBy { it.carExternalId }
            .mapValues { EventSummaryService.currentMileage(it.value) }
        val names = cars.associate { it.externalId to it.name }
        notifier.sync(
            ReminderAlertService.approachingReminders(reminders, mileageByCar).map { reminder ->
                reminder to (names[reminder.carExternalId].orEmpty())
            },
        )
    }

    fun scheduleDaily() {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        var target = ZonedDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(ZonedDateTime.now())) target = target.plusDays(1)
        val intent = Intent(context, ReminderAlertReceiver::class.java).setAction(ACTION_REMINDER_ALERT)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, target.toInstant().toEpochMilli(), pending)
    }
}

@AndroidEntryPoint
class ReminderAlertReceiver : BroadcastReceiver() {
    @Inject lateinit var monitor: ReminderAlertMonitor

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                monitor.refreshOnce()
                monitor.scheduleDaily()
            } finally {
                pending.finish()
            }
        }
    }
}
