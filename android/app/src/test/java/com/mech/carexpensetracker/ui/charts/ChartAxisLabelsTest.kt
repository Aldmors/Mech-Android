package com.mech.carexpensetracker.ui.charts

import com.mech.carexpensetracker.domain.model.ChartDatePreset
import com.mech.carexpensetracker.domain.model.ChartKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class ChartAxisLabelsTest {
    @Test
    fun addsYearForAllTimeAndRangesSpanningMultipleYears() {
        val labels = listOf("2025-12", "2026-01")

        assertEquals(
            listOf("Dec 2025", "Jan 2026"),
            chartAxisLabels(labels, ChartKind.MonthlySpending, ChartDatePreset.AllTime, Locale.ENGLISH),
        )
        assertEquals(
            listOf("Dec 2025", "Jan 2026"),
            chartAxisLabels(labels, ChartKind.MonthlySpending, ChartDatePreset.SixMonths, Locale.ENGLISH),
        )
    }

    @Test
    fun keepsDailyLabelsForConsumptionAndCumulativeCharts() {
        val labels = listOf("2026-03-01", "2026-03-15", "2026-04-02")
        assertEquals(
            labels,
            chartAxisLabels(labels, ChartKind.FuelConsumption, ChartDatePreset.TwelveMonths, Locale.ENGLISH),
        )
        assertEquals(
            labels,
            chartAxisLabels(labels, ChartKind.CumulativeCost, ChartDatePreset.AllTime, Locale.ENGLISH),
        )
    }
}
