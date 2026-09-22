package com.mech.carexpensetracker.ui.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class EventDatePickerTest {
    @Test
    fun pacificPickedDayDoesNotShift() {
        val zone = ZoneId.of("America/Los_Angeles")
        val original = LocalDate.of(2026, 3, 15).atTime(15, 30).atZone(zone).toInstant().toEpochMilli()
        val utcMidnight = utcMidnightMillis(original, zone)
        val stored = applyPickedUtcDate(original, utcMidnight, zone)
        assertEquals(LocalDate.of(2026, 3, 15), Instant.ofEpochMilli(stored).atZone(zone).toLocalDate())
        assertEquals(15, Instant.ofEpochMilli(stored).atZone(zone).hour)
    }

    @Test
    fun pickerUtcMidnightMapsToSameLocalDate() {
        val zone = ZoneId.of("America/Los_Angeles")
        val selected = LocalDate.of(2026, 3, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val original = LocalDate.of(2026, 3, 10).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val stored = applyPickedUtcDate(original, selected, zone)
        assertEquals(LocalDate.of(2026, 3, 15), Instant.ofEpochMilli(stored).atZone(zone).toLocalDate())
        assertEquals(9, Instant.ofEpochMilli(stored).atZone(zone).hour)
    }
}
