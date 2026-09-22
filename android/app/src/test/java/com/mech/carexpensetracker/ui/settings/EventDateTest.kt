package com.mech.carexpensetracker.ui.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class EventDateTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")

    @Test
    fun pickingADateKeepsTheLocalTime() {
        val current = LocalDate.of(2026, 9, 21).atTime(14, 32).atZone(warsaw).toInstant().toEpochMilli()
        val pickedUtc = LocalDate.of(2026, 9, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val applied = applyPickedUtcDate(current, pickedUtc, warsaw)

        val result = Instant.ofEpochMilli(applied).atZone(warsaw)
        assertEquals(LocalDate.of(2026, 9, 10), result.toLocalDate())
        assertEquals(LocalTime.of(14, 32), result.toLocalTime())
    }

    @Test
    fun utcMidnightUsesTheLocalCalendarDay() {
        val localMorning = LocalDate.of(2026, 9, 21).atTime(1, 15).atZone(warsaw).toInstant().toEpochMilli()
        val utc = utcMidnightMillis(localMorning, warsaw)
        assertEquals(
            LocalDate.of(2026, 9, 21),
            Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate(),
        )
    }
}
