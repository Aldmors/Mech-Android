package com.mech.carexpensetracker.reminders

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.domain.service.ReminderSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CalendarSyncResult {
    data class Ok(val added: Int) : CalendarSyncResult
    data object NoCalendar : CalendarSyncResult
    data object Failed : CalendarSyncResult
}

@Singleton
class ReminderCalendarSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun sync(
        reminders: List<CarReminderEntity>,
        currentMileage: Int?,
        avgDailyDistance: Double?,
        zone: ZoneId = ZoneId.systemDefault(),
        onUpdated: suspend (CarReminderEntity) -> Unit,
    ): CalendarSyncResult = withContext(Dispatchers.IO) {
        val calendarId = writableCalendarId() ?: return@withContext CalendarSyncResult.NoCalendar
        val today = LocalDate.now(zone)
        var added = 0
        try {
            reminders.forEach { reminder ->
                deleteSynced(reminder.syncedItemIdentifier)
                val ids = mutableListOf<Long>()
                reminder.dueDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                    insertAllDay(calendarId, reminder.title, date, reminder.colorHex)?.let { ids += it }
                }
                reminder.dueMileage?.let { due ->
                    ReminderSchedule.kmCalendarDate(due, currentMileage, today, avgDailyDistance)?.let { date ->
                        insertAllDay(calendarId, reminder.title, date, reminder.colorHex)?.let { ids += it }
                    }
                }
                added += ids.size
                onUpdated(reminder.copy(syncedItemIdentifier = ids.joinToString(",").ifEmpty { null }))
            }
            CalendarSyncResult.Ok(added)
        } catch (_: SecurityException) {
            CalendarSyncResult.Failed
        }
    }

    private fun writableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE}=1",
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val accessIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            while (cursor.moveToNext()) {
                if (cursor.getInt(accessIdx) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    return cursor.getLong(idIdx)
                }
            }
        }
        return null
    }

    private fun insertAllDay(
        calendarId: Long,
        title: String,
        date: LocalDate,
        colorHex: String,
    ): Long? {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.ALL_DAY, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            runCatching { put(CalendarContract.Events.EVENT_COLOR, android.graphics.Color.parseColor(colorHex)) }
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        return uri.lastPathSegment?.toLongOrNull()
    }

    private fun deleteSynced(raw: String?) {
        raw.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }.forEach { id ->
            runCatching {
                context.contentResolver.delete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                    null,
                    null,
                )
            }
        }
    }
}
