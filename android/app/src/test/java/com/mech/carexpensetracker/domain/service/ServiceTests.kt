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
        val byId = ConsumptionCalculator.previousFuelEvents(listOf(previous, partial, full))
        assertNull(
            ConsumptionCalculator.consumptionIfFullTank(
                partial,
                byId[partial.externalId],
                com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
            ),
        )
        assertEquals(
            BigDecimal("10.00"),
            ConsumptionCalculator.consumptionIfFullTank(
                full,
                byId[full.externalId],
                com.mech.carexpensetracker.domain.model.VehicleUnits.Km,
            ),
        )
    }

    private fun fuelEvent(
        id: String,
        dateMillis: Long,
        mileage: Int,
        amount: String = "10",
        fullTank: Boolean = true,
    ) = com.mech.carexpensetracker.data.db.entity.CarEventEntity(
        externalId = id,
        carExternalId = "c1",
        typeRaw = "fuel",
        dateMillis = dateMillis,
        mileage = mileage,
        fuelTypeRaw = "diesel",
        fuelAmount = amount,
        fuelFullTank = fullTank,
    )

    @Test
    fun mpg() {
        val result = ConsumptionCalculator.mpg(BigDecimal("10"), 300)
        assertEquals(BigDecimal("30.00"), result)
    }

    @Test
    fun nullWhenNoDistance() {
        assertNull(ConsumptionCalculator.litersPer100Km(BigDecimal("10"), 0))
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
        assertNull(CurrencyFormatter.parse(""))
    }
}

class RecordCostServiceTest {
    @Test
    fun unitPrice() {
        assertEquals(BigDecimal("2.00"), RecordCostService.unitPrice(BigDecimal("10"), BigDecimal("5")))
        assertNull(RecordCostService.unitPrice(BigDecimal("10"), BigDecimal.ZERO))
        assertNull(RecordCostService.unitPrice(null, BigDecimal("5")))
    }
}

class MileageValidationServiceTest {
    @Test
    fun rejectsLowerMileage() {
        val events = listOf(
            com.mech.carexpensetracker.data.db.entity.CarEventEntity(
                externalId = "1",
                carExternalId = "c1",
                typeRaw = "fuel",
                dateMillis = 1,
                mileage = 50000,
            ),
        )
        val result = MileageValidationService.validate(40000, events)
        assertEquals(false, result.isValid)
    }
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
