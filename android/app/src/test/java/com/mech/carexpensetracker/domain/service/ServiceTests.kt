package com.mech.carexpensetracker.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        assertNotNull(CurrencyFormatter.format(BigDecimal("10.5")))
    }

    @Test
    fun parse() {
        assertEquals(BigDecimal("12.50"), CurrencyFormatter.parse("12.50"))
        assertNull(CurrencyFormatter.parse(""))
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
