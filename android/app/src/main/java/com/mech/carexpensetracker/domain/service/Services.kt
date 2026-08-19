package com.mech.carexpensetracker.domain.service

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: BigDecimal?, locale: Locale = Locale.getDefault()): String {
        if (amount == null) return ""
        val formatter = NumberFormat.getCurrencyInstance(locale)
        return formatter.format(amount)
    }

    fun formatOrDash(amount: BigDecimal?, locale: Locale = Locale.getDefault()): String {
        if (amount == null) return "—"
        return format(amount, locale)
    }

    fun parse(input: String?): BigDecimal? {
        if (input.isNullOrBlank()) return null
        val cleaned = input.replace(Regex("[^\\d.,-]"), "").replace(",", ".")
        return cleaned.toBigDecimalOrNull()
    }

    fun currencySymbol(locale: Locale = Locale.getDefault()): String {
        return try {
            Currency.getInstance(locale).symbol
        } catch (_: Exception) {
            "$"
        }
    }
}

object RecordCostService {
    fun totalCost(
        fuelCost: BigDecimal?,
        secondaryFuelCost: BigDecimal?,
        partsCost: BigDecimal?,
        labourCost: BigDecimal?,
        explicitTotal: BigDecimal?,
    ): BigDecimal? {
        if (explicitTotal != null) return explicitTotal
        val parts = listOfNotNull(fuelCost, secondaryFuelCost, partsCost, labourCost)
        if (parts.isEmpty()) return null
        return parts.fold(BigDecimal.ZERO) { acc, v -> acc + v }
    }

    fun repairTotal(partsCost: BigDecimal?, labourCost: BigDecimal?): BigDecimal? {
        return totalCost(null, null, partsCost, labourCost, null)
    }
}

object EventSummaryService {
    data class MonthlySummary(
        val totalSpend: BigDecimal,
        val fuelSpend: BigDecimal,
        val repairSpend: BigDecimal,
        val papersSpend: BigDecimal,
        val eventCount: Int,
    )

    fun monthlySummary(
        events: List<com.mech.carexpensetracker.data.db.entity.CarEventEntity>,
        year: Int,
        month: Int,
    ): MonthlySummary {
        val monthEvents = events.filter {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.get(java.util.Calendar.YEAR) == year &&
                cal.get(java.util.Calendar.MONTH) + 1 == month
        }
        var fuel = BigDecimal.ZERO
        var repair = BigDecimal.ZERO
        var papers = BigDecimal.ZERO
        monthEvents.forEach { event ->
            val cost = event.totalCost?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            when (com.mech.carexpensetracker.domain.model.EventType.fromRaw(event.typeRaw)) {
                com.mech.carexpensetracker.domain.model.EventType.Fuel -> fuel += cost
                com.mech.carexpensetracker.domain.model.EventType.Repair -> repair += cost
                com.mech.carexpensetracker.domain.model.EventType.Papers -> papers += cost
            }
        }
        val total = fuel + repair + papers
        return MonthlySummary(total, fuel, repair, papers, monthEvents.size)
    }

    fun currentMileage(events: List<com.mech.carexpensetracker.data.db.entity.CarEventEntity>): Int? {
        return events.mapNotNull { it.mileage }.maxOrNull()
    }
}

object OwnershipAnalyticsService {
    data class OwnershipStats(
        val totalCost: BigDecimal,
        val costPerMonth: BigDecimal?,
        val costPerKm: BigDecimal?,
        val monthsOwned: Int,
        val totalKm: Int?,
    )

    fun compute(
        events: List<com.mech.carexpensetracker.data.db.entity.CarEventEntity>,
        buyDateMillis: Long?,
        units: com.mech.carexpensetracker.domain.model.VehicleUnits,
    ): OwnershipStats {
        val totalCost = events.mapNotNull { it.totalCost?.toBigDecimalOrNull() }
            .fold(BigDecimal.ZERO) { acc, v -> acc + v }
        val mileages = events.mapNotNull { it.mileage }.sorted()
        val totalDistance = if (mileages.size >= 2) mileages.last() - mileages.first() else null
        val now = System.currentTimeMillis()
        val start = buyDateMillis ?: events.minOfOrNull { it.dateMillis } ?: now
        val monthsOwned = maxOf(
            1,
            ((now - start) / (30L * 24 * 60 * 60 * 1000)).toInt(),
        )
        val costPerMonth = totalCost.divide(BigDecimal(monthsOwned), 2, RoundingMode.HALF_UP)
        val costPerKm = if (totalDistance != null && totalDistance > 0) {
            totalCost.divide(BigDecimal(totalDistance), 2, RoundingMode.HALF_UP)
        } else {
            null
        }
        return OwnershipStats(totalCost, costPerMonth, costPerKm, monthsOwned, totalDistance)
    }
}

object PlanningSavingsService {
    data class SavingsSummary(
        val totalPlanned: BigDecimal,
        val monthlyTarget: BigDecimal?,
        val monthsToTarget: Int?,
    )

    fun summarize(
        planned: List<com.mech.carexpensetracker.data.db.entity.PlannedExpenseEntity>,
    ): SavingsSummary {
        val total = planned.mapNotNull { it.cost.toBigDecimalOrNull() }
            .fold(BigDecimal.ZERO) { acc, v -> acc + v }
        val maxMonths = planned.mapNotNull { it.months }.maxOrNull()
        val monthly = if (maxMonths != null && maxMonths > 0) {
            total.divide(BigDecimal(maxMonths), 2, RoundingMode.HALF_UP)
        } else {
            null
        }
        return SavingsSummary(total, monthly, maxMonths)
    }
}

object NoteSortingService {
    fun sort(
        notes: List<com.mech.carexpensetracker.data.db.entity.CarNoteEntity>,
    ): List<com.mech.carexpensetracker.data.db.entity.CarNoteEntity> {
        val priorityOrder = mapOf("high" to 0, "normal" to 1, "low" to 2)
        return notes.sortedWith(
            compareBy<com.mech.carexpensetracker.data.db.entity.CarNoteEntity> {
                if (it.isResolved) 1 else 0
            }.thenBy {
                priorityOrder[it.priorityRaw] ?: 1
            }.thenByDescending { it.createdAtMillis },
        )
    }
}

object ObligatoryReminderService {
    val templates = listOf(
        "Insurance renewal",
        "Technical inspection",
        "Oil change",
        "Tire rotation",
    )

    fun seedForCar(carExternalId: String): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        return templates.mapIndexed { index, title ->
            com.mech.carexpensetracker.data.db.entity.CarReminderEntity(
                externalId = "obligatory-$carExternalId-$index",
                carExternalId = carExternalId,
                title = title,
                isObligatory = true,
            )
        }
    }
}

object ReminderAlertService {
    fun mileageRemindersDue(
        reminders: List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity>,
        currentMileage: Int?,
    ): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        if (currentMileage == null) return emptyList()
        return reminders.filter {
            !it.isCompleted && it.dueMileage != null && currentMileage >= it.dueMileage
        }
    }
}
