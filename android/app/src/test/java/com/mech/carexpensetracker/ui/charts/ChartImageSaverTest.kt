package com.mech.carexpensetracker.ui.charts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChartImageSaverTest {
    @Test
    fun clampsToVisibleWindow() {
        val region = clampedCaptureRect(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 80f,
            viewWidth = 1000,
            viewHeight = 2000,
        )
        assertEquals(CaptureRegion(10, 20, 100, 60), region)
    }

    @Test
    fun rejectsEmptyRegion() {
        assertNull(
            clampedCaptureRect(
                left = 0f,
                top = 0f,
                right = 0f,
                bottom = 10f,
                viewWidth = 100,
                viewHeight = 100,
            ),
        )
    }
}
