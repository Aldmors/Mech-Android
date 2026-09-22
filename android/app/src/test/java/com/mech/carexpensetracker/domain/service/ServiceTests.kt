package com.mech.carexpensetracker.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ConsumptionCalculatorTest {
    @Test
    fun litersPer100Km() {
        val result = ConsumptionCalculator.litersPer100Km(BigDecimal("10"), 100)
        assertEquals(BigDecimal("10.00"), result)
    }

    @Test
    fun previousFuelEventsMapsChronologicalNeighbors() {
        val first = fuelEvent("a", dateMillis = 1, mileage = 1000)
        val second = fuelEvent("b", dateMillis = 2, mileage = 1100)
        val third = fuelEvent("c", dateMillis = 3, mileage = 1200)
        val previous = ConsumptionCalculator.previousFuelEvents(listOf(third, first, second))
        assertEquals(first.externalId, previous[second.externalId]?.externalId)
        assertEquals(second.externalId, previous[third.externalId]?.externalId)
        assertNull(previous[first.externalId])
    }

    @Test
    fun consumptionOnlyWhenCurrentFillIsFullTank() {
        val previous = fuelEvent("a", dateMillis = 1, mileage = 1000)
        val partial = fuelEvent("b", dateMillis = 2, mileage = 1100, fullTank = false)
        val full = fuelEvent("c", dateMillis = 3, mileage = 1200)
        val byId = ConsumptionCalculator.consumptionByEventId(
            listOf(previous, partial, full),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertNull(byId[partial.externalId])
        assertEquals(BigDecimal("10.00"), byId[full.externalId])
    }

    @Test
    fun fullToFullAddsPartialFillsInBetween() {
        val firstFull = fuelEvent("a", dateMillis = 1, mileage = 1000, amount = "40")
        val partial = fuelEvent("b", dateMillis = 2, mileage = 1100, amount = "5", fullTank = false)
        val secondFull = fuelEvent("c", dateMillis = 3, mileage = 1200, amount = "35")
        val byId = ConsumptionCalculator.consumptionByEventId(
            listOf(firstFull, partial, secondFull),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertEquals(BigDecimal("20.00"), byId[secondFull.externalId])
    }

    private fun fuelEvent(
        id: String,
        dateMillis: Long,
        mileage: Int,
        amount: String? = "10",
        fullTank: Boolean = true,
        secondaryAmount: String? = null,
        secondaryFullTank: Boolean = false,
    ) = com.mech.carexpensetracker.data.db.entity.CarEventEntity(
        externalId = id,
        carExternalId = "c1",
        typeRaw = "fuel",
        dateMillis = dateMillis,
        mileage = mileage,
        fuelTypeRaw = if (amount != null) "diesel" else null,
        fuelAmount = amount,
        fuelFullTank = fullTank,
        secondaryFuelTypeRaw = if (secondaryAmount != null) "lpg" else null,
        secondaryFuelAmount = secondaryAmount,
        secondaryFuelFullTank = secondaryFullTank,
    )

    @Test
    fun firstFullTankHasNoConsumption() {
        val first = fuelEvent("a", dateMillis = 1, mileage = 1000, amount = "40")
        val second = fuelEvent("b", dateMillis = 2, mileage = 1100, amount = "10")
        val byId = ConsumptionCalculator.consumptionByEventId(
            listOf(first, second),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertNull(byId[first.externalId])
        assertEquals(BigDecimal("10.00"), byId[second.externalId])
    }

    @Test
    fun noConsumptionUntilBaselineFullTank() {
        val partial = fuelEvent("a", dateMillis = 1, mileage = 1000, amount = "10", fullTank = false)
        val full = fuelEvent("b", dateMillis = 2, mileage = 1100, amount = "10")
        val byId = ConsumptionCalculator.consumptionByEventId(
            listOf(partial, full),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertNull(byId[partial.externalId])
        assertNull(byId[full.externalId])
    }

    @Test
    fun averageConsumptionIsDistanceWeighted() {
        val first = fuelEvent("a", dateMillis = 1, mileage = 0, amount = "10")
        val short = fuelEvent("b", dateMillis = 2, mileage = 100, amount = "10")
        val long = fuelEvent("c", dateMillis = 3, mileage = 500, amount = "20")
        val average = ConsumptionCalculator.averageConsumption(
            listOf(first, short, long),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertEquals(BigDecimal("6.00"), average)
    }

    @Test
    fun mpg() {
        val result = ConsumptionCalculator.mpg(BigDecimal("10"), 300)
        assertEquals(BigDecimal("30.00"), result)
    }

    @Test
    fun nullWhenNoDistance() {
        assertNull(ConsumptionCalculator.litersPer100Km(BigDecimal("10"), 0))
    }

    @Test
    fun primaryConsumptionIgnoresSecondaryFills() {
        val first = fuelEvent("a", dateMillis = 1, mileage = 1000, amount = "10")
        val lpg = fuelEvent(
            "b",
            dateMillis = 2,
            mileage = 1100,
            amount = null,
            secondaryAmount = "50",
            secondaryFullTank = true,
        )
        val second = fuelEvent("c", dateMillis = 3, mileage = 1200, amount = "10")
        val primary = ConsumptionCalculator.consumptionByEventId(
            listOf(first, lpg, second),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertEquals(BigDecimal("5.00"), primary[second.externalId])
        assertNull(primary[lpg.externalId])
    }

    @Test
    fun secondaryConsumptionIgnoresPrimaryFills() {
        val firstLpg = fuelEvent(
            "a",
            dateMillis = 1,
            mileage = 1000,
            amount = null,
            fullTank = false,
            secondaryAmount = "20",
            secondaryFullTank = true,
        )
        val gasoline = fuelEvent("b", dateMillis = 2, mileage = 1100, amount = "40")
        val secondLpg = fuelEvent(
            "c",
            dateMillis = 3,
            mileage = 1200,
            amount = null,
            fullTank = false,
            secondaryAmount = "20",
            secondaryFullTank = true,
        )
        val secondary = ConsumptionCalculator.consumptionByEventId(
            listOf(firstLpg, gasoline, secondLpg),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
            com.mech.carexpensetracker.domain.model.FuelSlot.Secondary,
        )
        assertEquals(BigDecimal("10.00"), secondary[secondLpg.externalId])
        assertNull(secondary[gasoline.externalId])
    }

    @Test
    fun displayedConsumptionUsesSecondaryWhenPrimaryMissing() {
        val first = fuelEvent(
            "a",
            dateMillis = 1,
            mileage = 1000,
            amount = null,
            fullTank = false,
            secondaryAmount = "30",
            secondaryFullTank = true,
        )
        val second = fuelEvent(
            "b",
            dateMillis = 2,
            mileage = 1100,
            amount = null,
            fullTank = false,
            secondaryAmount = "30",
            secondaryFullTank = true,
        )
        val displayed = ConsumptionCalculator.displayedConsumptionByEventId(
            listOf(first, second),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertEquals(BigDecimal("30.00"), displayed[second.externalId])
    }

    @Test
    fun primarySlotDoesNotMixFuelTypes() {
        val gas1 = fuelEvent("g1", dateMillis = 1, mileage = 1000, amount = "40").copy(fuelTypeRaw = "gasoline")
        val lpg1 = fuelEvent("l1", dateMillis = 2, mileage = 1100, amount = "30").copy(fuelTypeRaw = "lpg")
        val gas2 = fuelEvent("g2", dateMillis = 3, mileage = 2000, amount = "40").copy(fuelTypeRaw = "gasoline")
        val lpg2 = fuelEvent("l2", dateMillis = 4, mileage = 2100, amount = "30").copy(fuelTypeRaw = "lpg")
        val byId = ConsumptionCalculator.displayedConsumptionByEventId(
            listOf(gas1, lpg1, gas2, lpg2),
            com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertEquals(BigDecimal("4.00"), byId[gas2.externalId])
        assertEquals(BigDecimal("3.00"), byId[lpg2.externalId])
    }
}

class CurrencyFormatterTest {
    @Test
    fun formatOrDash() {
        assertEquals("—", CurrencyFormatter.formatOrDash(null))
        assertEquals("10,50 zł", CurrencyFormatter.format(BigDecimal("10.50")).replace('\u00A0', ' '))
    }

    @Test
    fun parse() {
        assertEquals(BigDecimal("12.50"), CurrencyFormatter.parse("12.50"))
        assertEquals(BigDecimal("12.50"), CurrencyFormatter.parse("12,50"))
        assertEquals(BigDecimal("1234.56"), CurrencyFormatter.parse("1.234,56"))
        assertEquals(BigDecimal("1234.56"), CurrencyFormatter.parse("1,234.56"))
        assertNull(CurrencyFormatter.parse(""))
        assertNull(CurrencyFormatter.parseStored("-1"))
        assertNull(CurrencyFormatter.parseStored("-5"))
    }
}

class RecordCostServiceTest {
    @Test
    fun unitPrice() {
        assertEquals(BigDecimal("2.00"), RecordCostService.unitPrice(BigDecimal("10"), BigDecimal("5")))
        assertEquals(BigDecimal("6.01"), RecordCostService.unitPrice(BigDecimal("240.50"), BigDecimal("40")))
        assertNull(RecordCostService.unitPrice(BigDecimal("10"), BigDecimal.ZERO))
        assertNull(RecordCostService.unitPrice(null, BigDecimal("5")))
    }

    @Test
    fun totalCostSumsFuelLegs() {
        assertEquals(
            BigDecimal("15.50"),
            RecordCostService.totalCost(
                BigDecimal("10.00"),
                BigDecimal("5.50"),
                null,
                null,
                null,
            ),
        )
        assertEquals(
            BigDecimal("7.20"),
            RecordCostService.totalCost(null, BigDecimal("7.20"), null, null, null),
        )
        assertNull(RecordCostService.totalCost(null, null, null, null, null))
    }

    @Test
    fun repairTotalSumsBreakdownOrUsesExplicit() {
        assertEquals(BigDecimal("30"), RecordCostService.repairTotal(BigDecimal("10"), BigDecimal("20")))
        assertEquals(BigDecimal("10"), RecordCostService.repairTotal(BigDecimal("10"), null))
        assertEquals(BigDecimal("50"), RecordCostService.repairTotal(null, null, BigDecimal("50")))
        assertEquals(
            BigDecimal("30"),
            RecordCostService.repairTotal(BigDecimal("10"), BigDecimal("20"), BigDecimal("99")),
        )
        assertNull(RecordCostService.repairTotal(null, null, null))
    }
}

class MileageValidationServiceTest {
    @Test
    fun rejectsMileageBelowRecordedMax() {
        val result = MileageValidationService.validate(40000, listOf(event(mileage = 50000)))
        assertEquals(false, result.isValid)
        assertEquals(50000, result.minAllowed)
    }

    @Test
    fun allowsMileageEqualToRecordedMax() {
        val result = MileageValidationService.validate(50000, listOf(event(mileage = 50000)))
        assertEquals(true, result.isValid)
        assertNull(result.minAllowed)
    }

    @Test
    fun rejectsNegativeMileage() {
        val result = MileageValidationService.validate(-1, emptyList())
        assertEquals(false, result.isValid)
        assertNull(result.minAllowed)
    }

    @Test
    fun excludeExternalIdDoesNotCompareAgainstSelf() {
        val result = MileageValidationService.validate(
            40000,
            listOf(event(id = "1", mileage = 50000)),
            excludeExternalId = "1",
        )
        assertEquals(true, result.isValid)
        assertNull(result.minAllowed)
    }

    @Test
    fun excludeStillEnforcesOtherEventsMax() {
        val events = listOf(
            event(id = "1", mileage = 50000),
            event(id = "2", mileage = 40000),
        )
        val result = MileageValidationService.validate(30000, events, excludeExternalId = "1")
        assertEquals(false, result.isValid)
        assertEquals(40000, result.minAllowed)
    }

    @Test
    fun newestUsesGlobalMaxEvenWhenPredecessorIsLower() {
        val events = listOf(
            event(id = "old", mileage = 80000, dateMillis = 1),
            event(id = "mid", mileage = 20000, dateMillis = 2),
            event(id = "new", mileage = 50000, dateMillis = 3),
        )
        val result = MileageValidationService.validate(
            40000,
            events,
            excludeExternalId = "new",
            dateMillis = 3,
        )
        assertEquals(false, result.isValid)
        assertEquals(80000, result.minAllowed)
    }

    @Test
    fun newRecordOnLatestDateUsesGlobalMax() {
        val events = listOf(
            event(id = "old", mileage = 10000, dateMillis = 1),
            event(id = "new", mileage = 50000, dateMillis = 3),
        )
        val result = MileageValidationService.validate(40000, events, dateMillis = 4)
        assertEquals(false, result.isValid)
        assertEquals(50000, result.minAllowed)
    }

    @Test
    fun oldRecordAllowsMileageBelowGlobalMaxIfAtLeastPredecessor() {
        val events = listOf(
            event(id = "old", mileage = 10000, dateMillis = 1),
            event(id = "mid", mileage = 20000, dateMillis = 2),
            event(id = "new", mileage = 50000, dateMillis = 3),
        )
        val result = MileageValidationService.validate(
            15000,
            events,
            excludeExternalId = "mid",
            dateMillis = 2,
        )
        assertEquals(true, result.isValid)
        assertNull(result.minAllowed)
    }

    @Test
    fun oldRecordRejectsMileageBelowPredecessor() {
        val events = listOf(
            event(id = "old", mileage = 10000, dateMillis = 1),
            event(id = "mid", mileage = 20000, dateMillis = 2),
            event(id = "new", mileage = 50000, dateMillis = 3),
        )
        val result = MileageValidationService.validate(
            9000,
            events,
            excludeExternalId = "mid",
            dateMillis = 2,
        )
        assertEquals(false, result.isValid)
        assertEquals(10000, result.minAllowed)
    }

    @Test
    fun backdatedNewRecordUsesPredecessorNotGlobalMax() {
        val events = listOf(
            event(id = "old", mileage = 10000, dateMillis = 1),
            event(id = "new", mileage = 50000, dateMillis = 3),
        )
        val result = MileageValidationService.validate(15000, events, dateMillis = 2)
        assertEquals(true, result.isValid)
        assertNull(result.minAllowed)
    }

    @Test
    fun oldestRecordHasNoPredecessorMinimum() {
        val events = listOf(
            event(id = "old", mileage = 10000, dateMillis = 1),
            event(id = "new", mileage = 50000, dateMillis = 3),
        )
        val result = MileageValidationService.validate(
            1000,
            events,
            excludeExternalId = "old",
            dateMillis = 1,
        )
        assertEquals(true, result.isValid)
        assertNull(result.minAllowed)
    }

    private fun event(id: String = "1", mileage: Int, dateMillis: Long = 1) =
        com.mech.carexpensetracker.data.db.entity.CarEventEntity(
            externalId = id,
            carExternalId = "c1",
            typeRaw = "fuel",
            dateMillis = dateMillis,
            mileage = mileage,
        )
}

class OwnershipAnalyticsServiceTest {
    @Test
    fun costPerMonthDividesTotalByMonthsSinceFirstRecord() {
        val twoMonthsAgo = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000
        val events = listOf(
            event("1", twoMonthsAgo, "100"),
            event("2", System.currentTimeMillis(), "100"),
        )
        val stats = OwnershipAnalyticsService.compute(
            events,
            units = com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
        )
        assertEquals(BigDecimal("200"), stats.totalCost)
        assertEquals(2, stats.monthsOwned)
        assertEquals(BigDecimal("100.00"), stats.costPerMonth)
    }

    private fun event(id: String, dateMillis: Long, cost: String) =
        com.mech.carexpensetracker.data.db.entity.CarEventEntity(
            externalId = id,
            carExternalId = "c1",
            typeRaw = "fuel",
            dateMillis = dateMillis,
            totalCost = cost,
        )
}

class PlanningSavingsServiceTest {
    @Test
    fun summarizePlannedExpenses() {
        val planned = listOf(
            com.mech.carexpensetracker.data.db.entity.PlannedExpenseEntity(
                externalId = "p1",
                carExternalId = "c1",
                name = "Tires",
                cost = "600",
                months = 6,
                createdAtMillis = 1,
            ),
        )
        val summary = PlanningSavingsService.summarize(planned)
        assertEquals(BigDecimal("600"), summary.totalPlanned)
        assertEquals(BigDecimal("100.00"), summary.monthlyTarget)
    }

    @Test
    fun monthlyTargetSumsEachItem() {
        val planned = listOf(
            com.mech.carexpensetracker.data.db.entity.PlannedExpenseEntity(
                externalId = "p1",
                carExternalId = "c1",
                name = "Tires",
                cost = "600",
                months = 6,
                createdAtMillis = 1,
            ),
            com.mech.carexpensetracker.data.db.entity.PlannedExpenseEntity(
                externalId = "p2",
                carExternalId = "c1",
                name = "Battery",
                cost = "1200",
                months = 12,
                createdAtMillis = 2,
            ),
        )
        val summary = PlanningSavingsService.summarize(planned)
        assertEquals(BigDecimal("1800"), summary.totalPlanned)
        assertEquals(BigDecimal("200.00"), summary.monthlyTarget)
    }
}

class ReminderAlertServiceTest {
    @Test
    fun dueByMileageOrDate() {
        val reminders = listOf(
            reminder("Oil", dueMileage = 100),
            reminder("Insurance", dueDateMillis = 1),
            reminder("Later", dueDateMillis = 99),
            reminder("Done", dueMileage = 1, completed = true),
        )
        val due = ReminderAlertService.dueReminders(reminders, currentMileage = 100, nowMillis = 10)
        assertEquals(listOf("Oil", "Insurance"), due.map { it.title })
    }

    @Test
    fun approachingWithinSevenDaysOrUnder200Km() {
        val zone = java.time.ZoneId.of("UTC")
        val now = java.time.LocalDate.of(2026, 6, 10).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val inSeven = java.time.LocalDate.of(2026, 6, 17).atStartOfDay(zone).toInstant().toEpochMilli()
        val inEight = java.time.LocalDate.of(2026, 6, 18).atStartOfDay(zone).toInstant().toEpochMilli()
        val reminders = listOf(
            reminder("Soon", dueDateMillis = inSeven),
            reminder("Later", dueDateMillis = inEight),
            reminder("KmSoon", dueMileage = 10_199),
            reminder("KmLater", dueMileage = 10_200),
            reminder("Either", dueDateMillis = inEight, dueMileage = 10_199),
            reminder("Done", dueDateMillis = inSeven, completed = true),
        )
        val approaching = ReminderAlertService.approachingReminders(
            reminders,
            currentMileage = 10_000,
            nowMillis = now,
            zone = zone,
        )
        assertEquals(listOf("Soon", "KmSoon", "Either"), approaching.map { it.title })
    }

    private fun reminder(
        title: String,
        dueMileage: Int? = null,
        dueDateMillis: Long? = null,
        completed: Boolean = false,
    ) = com.mech.carexpensetracker.data.db.entity.CarReminderEntity(
        externalId = title,
        carExternalId = "c1",
        title = title,
        dueMileage = dueMileage,
        dueDateMillis = dueDateMillis,
        isCompleted = completed,
    )
}

class ReminderScheduleTest {
    @Test
    fun dueDateUsesCalendarYears() {
        val zone = java.time.ZoneId.of("UTC")
        val start = java.time.LocalDate.of(2024, 2, 29)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val due = ReminderSchedule.dueDateAfterYears(1, start, zone)
        val dueDate = java.time.Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
        assertEquals(java.time.LocalDate.of(2025, 2, 28), dueDate)
    }

    @Test
    fun dueMileageCountsFromCurrentOrZero() {
        assertEquals(60_000, ReminderSchedule.dueMileageAfterInterval(50_000, 10_000))
        assertEquals(10_000, ReminderSchedule.dueMileageAfterInterval(null, 10_000))
    }

    @Test
    fun nextReminderShiftsByOriginalInterval() {
        val zone = java.time.ZoneId.of("UTC")
        val created = java.time.LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val due = java.time.LocalDate.of(2026, 2, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = java.time.LocalDate.of(2026, 1, 20).atStartOfDay(zone).toInstant().toEpochMilli()
        val original = com.mech.carexpensetracker.data.db.entity.CarReminderEntity(
            externalId = "r1",
            carExternalId = "c1",
            title = "Oil",
            dueDateMillis = due,
            dueMileage = 60_000,
            createdAtMillis = created,
            intervalDays = 31,
            intervalKm = 10_000,
            colorHex = "#F44336",
        )
        val next = ReminderSchedule.nextReminder(
            original,
            nowMillis = now,
            currentMileage = 55_000,
            newExternalId = "r2",
            zone = zone,
        )
        val nextDate = java.time.Instant.ofEpochMilli(next!!.dueDateMillis!!).atZone(zone).toLocalDate()
        assertEquals(java.time.LocalDate.of(2026, 2, 20), nextDate)
        assertEquals(65_000, next.dueMileage)
        assertEquals("#F44336", next.colorHex)
        assertEquals(false, next.isCompleted)
        assertEquals("Oil", next.title)
    }

    @Test
    fun kmCalendarIsTwoDaysAfterHundredKmLeft() {
        val today = java.time.LocalDate.of(2026, 6, 10)
        assertEquals(
            today.plusDays(2),
            ReminderSchedule.kmCalendarDate(10_080, 10_000, today, avgDailyDistance = 50.0),
        )
        assertEquals(
            today.plusDays(6),
            ReminderSchedule.kmCalendarDate(10_300, 10_000, today, avgDailyDistance = 50.0),
        )
        assertEquals(null, ReminderSchedule.kmCalendarDate(10_300, 10_000, today, avgDailyDistance = null))
    }
}

class ChartDataServiceTest {
    @Test
    fun categoryBreakdownUsesFourEventTypes() {
        val events = listOf(
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = "f",
                carExternalId = "c1",
                typeRaw = "fuel",
                dateMillis = 1,
                categoryName = "Ignored",
                totalCost = "10",
            ),
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = "r",
                carExternalId = "c1",
                typeRaw = "repair",
                dateMillis = 2,
                categoryName = "Oil",
                totalCost = "20",
            ),
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = "p",
                carExternalId = "c1",
                typeRaw = "papers",
                dateMillis = 3,
                categoryName = "OC",
                totalCost = "30",
            ),
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = "g",
                carExternalId = "c1",
                typeRaw = "care",
                dateMillis = 4,
                categoryName = "Myjnia",
                totalCost = "40",
            ),
        )
        val slices = ChartDataService.categoryBreakdown(events)
        assertEquals(
            listOf("Paliwo", "Serwis", "Dokumenty", "Pielęgnacja"),
            slices.map { it.name },
        )
        assertEquals(
            listOf(BigDecimal("10"), BigDecimal("20"), BigDecimal("30"), BigDecimal("40")),
            slices.map { it.amount },
        )
    }

    @Test
    fun monthlySpendingSplitsFourTypes() {
        val dateMillis = java.time.LocalDate.of(2026, 3, 15)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val events = listOf(
            "fuel" to "10",
            "repair" to "20",
            "papers" to "30",
            "care" to "40",
        ).mapIndexed { index, (type, cost) ->
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = "$index",
                carExternalId = "c1",
                typeRaw = type,
                dateMillis = dateMillis,
                totalCost = cost,
            )
        }
        val bar = ChartDataService.monthlySpending(events).single()
        assertEquals(BigDecimal("10"), bar.fuel)
        assertEquals(BigDecimal("20"), bar.repair)
        assertEquals(BigDecimal("30"), bar.papers)
        assertEquals(BigDecimal("40"), bar.care)
    }
}
