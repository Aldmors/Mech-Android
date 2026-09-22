package com.mech.carexpensetracker.domain.service

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    private val plnLocale = Locale.forLanguageTag("pl-PL")

    fun format(amount: BigDecimal?, locale: Locale = plnLocale): String {
        if (amount == null) return ""
        val formatter = NumberFormat.getCurrencyInstance(locale)
        return formatter.format(amount).replace('\u00A0', ' ').replace('\u202F', ' ')
    }

    fun formatOrDash(amount: BigDecimal?, locale: Locale = plnLocale): String {
        if (amount == null) return "—"
        return format(amount, locale)
    }

    fun parse(input: String?): BigDecimal? {
        if (input.isNullOrBlank()) return null
        val cleaned = input.replace(Regex("[^\\d.,-]"), "")
        if (cleaned.isEmpty() || cleaned == "-") return null
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(",", ".")
                } else {
                    cleaned.replace(",", "")
                }
            }
            lastComma >= 0 -> cleaned.replace(",", ".")
            else -> cleaned
        }
        return normalized.toBigDecimalOrNull()
    }

    fun parseStored(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank() || raw == "-1") return null
        val value = parse(raw) ?: return null
        if (value.signum() < 0) return null
        return value
    }

    fun currencySymbol(locale: Locale = plnLocale): String {
        return try {
            Currency.getInstance("PLN").getSymbol(locale)
        } catch (_: Exception) {
            "zł"
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

    fun repairTotal(
        partsCost: BigDecimal?,
        labourCost: BigDecimal?,
        explicitTotal: BigDecimal? = null,
    ): BigDecimal? {
        return totalCost(null, null, partsCost, labourCost, null) ?: explicitTotal
    }

    fun unitPrice(cost: BigDecimal?, amount: BigDecimal?): BigDecimal? {
        if (cost == null || amount == null || amount.signum() <= 0) return null
        return cost.divide(amount, 2, RoundingMode.HALF_UP)
    }
}

object EventSummaryService {
    data class MonthlySummary(
        val totalSpend: BigDecimal,
        val fuelSpend: BigDecimal,
        val repairSpend: BigDecimal,
        val papersSpend: BigDecimal,
        val careSpend: BigDecimal,
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
        var care = BigDecimal.ZERO
        monthEvents.forEach { event ->
            val cost = CurrencyFormatter.parseStored(event.totalCost) ?: BigDecimal.ZERO
            when (com.mech.carexpensetracker.domain.model.EventType.fromRaw(event.typeRaw)) {
                com.mech.carexpensetracker.domain.model.EventType.Fuel -> fuel += cost
                com.mech.carexpensetracker.domain.model.EventType.Repair -> repair += cost
                com.mech.carexpensetracker.domain.model.EventType.Papers -> papers += cost
                com.mech.carexpensetracker.domain.model.EventType.Care -> care += cost
            }
        }
        val total = fuel + repair + papers + care
        return MonthlySummary(total, fuel, repair, papers, care, monthEvents.size)
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
        units: com.mech.carexpensetracker.domain.model.VehicleUnits,
    ): OwnershipStats {
        val totalCost = events.mapNotNull { CurrencyFormatter.parseStored(it.totalCost) }
            .fold(BigDecimal.ZERO) { acc, v -> acc + v }
        val mileages = events.mapNotNull { it.mileage }.sorted()
        val totalDistance = if (mileages.size >= 2) mileages.last() - mileages.first() else null
        val now = System.currentTimeMillis()
        val start = events.minOfOrNull { it.dateMillis } ?: now
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
        val total = planned.mapNotNull { CurrencyFormatter.parseStored(it.cost) }
            .fold(BigDecimal.ZERO) { acc, v -> acc + v }
        val monthlyParts = planned.mapNotNull { item ->
            val cost = CurrencyFormatter.parseStored(item.cost) ?: return@mapNotNull null
            val months = item.months ?: return@mapNotNull null
            if (months <= 0) return@mapNotNull null
            cost.divide(BigDecimal(months), 2, RoundingMode.HALF_UP)
        }
        val monthly = monthlyParts.takeIf { it.isNotEmpty() }
            ?.fold(BigDecimal.ZERO) { acc, v -> acc + v }
        val maxMonths = planned.mapNotNull { it.months }.filter { it > 0 }.maxOrNull()
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

object ReminderSchedule {
    const val DATE_APPROACH_DAYS = 7
    const val MILEAGE_APPROACH_KM = 200
    const val KM_CALENDAR_REMAINING = 100
    const val KM_CALENDAR_OFFSET_DAYS = 2

    fun dueDateAfterYears(
        years: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Long {
        return java.time.Instant.ofEpochMilli(nowMillis)
            .atZone(zone)
            .toLocalDate()
            .plusYears(years.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    fun dueDateAfterDays(
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Long {
        return java.time.Instant.ofEpochMilli(nowMillis)
            .atZone(zone)
            .toLocalDate()
            .plusDays(days.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    fun dueMileageAfterInterval(currentMileage: Int?, interval: Int): Int {
        return (currentMileage ?: 0) + interval
    }

    fun intervalDaysBetween(
        fromMillis: Long,
        toMillis: Long,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Int {
        val from = java.time.Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
        val to = java.time.Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt().coerceAtLeast(1)
    }

    fun daysUntilDue(
        dueMillis: Long,
        nowMillis: Long,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Long {
        val due = java.time.Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalDate()
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(now, due)
    }

    fun averageDailyDistance(
        events: List<com.mech.carexpensetracker.data.db.entity.CarEventEntity>,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): Double? {
        val points = events.mapNotNull { event ->
            val mileage = event.mileage ?: return@mapNotNull null
            mileage to event.dateMillis
        }.sortedBy { it.second }
        if (points.size < 2) return null
        val distance = points.last().first - points.first().first
        if (distance <= 0) return null
        val start = java.time.Instant.ofEpochMilli(points.first().second).atZone(zone).toLocalDate()
        val end = java.time.Instant.ofEpochMilli(points.last().second).atZone(zone).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
        return distance.toDouble() / days
    }

    fun kmCalendarDate(
        dueMileage: Int,
        currentMileage: Int?,
        nowDate: java.time.LocalDate,
        avgDailyDistance: Double?,
    ): java.time.LocalDate? {
        val remaining = dueMileage - (currentMileage ?: 0)
        if (remaining <= KM_CALENDAR_REMAINING) {
            return nowDate.plusDays(KM_CALENDAR_OFFSET_DAYS.toLong())
        }
        if (avgDailyDistance == null || avgDailyDistance <= 0.0) return null
        // ponytail: whole-history average daily distance; swap for a 30-day window if estimates drift
        val kmUntilThreshold = remaining - KM_CALENDAR_REMAINING
        val daysUntilThreshold = kotlin.math.ceil(kmUntilThreshold / avgDailyDistance).toLong()
        return nowDate.plusDays(daysUntilThreshold + KM_CALENDAR_OFFSET_DAYS)
    }

    fun nextReminder(
        reminder: com.mech.carexpensetracker.data.db.entity.CarReminderEntity,
        nowMillis: Long,
        currentMileage: Int?,
        newExternalId: String,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): com.mech.carexpensetracker.data.db.entity.CarReminderEntity? {
        val intervalDays = reminder.intervalDays
            ?: reminder.dueDateMillis?.let { due ->
                reminder.createdAtMillis.takeIf { it > 0 }?.let { created ->
                    intervalDaysBetween(created, due, zone)
                }
            }
        val intervalKm = reminder.intervalKm
        if (intervalDays == null && intervalKm == null) return null
        return reminder.copy(
            id = 0,
            externalId = newExternalId,
            createdAtMillis = nowMillis,
            intervalDays = intervalDays,
            intervalKm = intervalKm,
            dueDateMillis = intervalDays?.let { dueDateAfterDays(it, nowMillis, zone) },
            dueMileage = intervalKm?.let { dueMileageAfterInterval(currentMileage, it) },
            isCompleted = false,
            syncedItemIdentifier = null,
            isObligatory = false,
        )
    }
}

data class ReminderStatus(
    val reminder: com.mech.carexpensetracker.data.db.entity.CarReminderEntity,
    val isDue: Boolean,
    val isApproaching: Boolean,
    val daysLeft: Long?,
    val remainingKm: Int?,
)

object ReminderAlertService {
    fun status(
        reminder: com.mech.carexpensetracker.data.db.entity.CarReminderEntity,
        currentMileage: Int?,
        nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): ReminderStatus {
        val daysLeft = reminder.dueDateMillis?.let { ReminderSchedule.daysUntilDue(it, nowMillis, zone) }
        val remainingKm = reminder.dueMileage?.let { due -> currentMileage?.let { due - it } }
        if (reminder.isCompleted) {
            return ReminderStatus(reminder, isDue = false, isApproaching = false, daysLeft, remainingKm)
        }
        val dateDue = reminder.dueDateMillis != null && reminder.dueDateMillis <= nowMillis
        val kmDue = remainingKm != null && remainingKm <= 0
        val dateApproaching = daysLeft != null && daysLeft <= ReminderSchedule.DATE_APPROACH_DAYS
        val kmApproaching = remainingKm != null && remainingKm < ReminderSchedule.MILEAGE_APPROACH_KM
        return ReminderStatus(
            reminder = reminder,
            isDue = dateDue || kmDue,
            isApproaching = dateApproaching || kmApproaching,
            daysLeft = daysLeft,
            remainingKm = remainingKm,
        )
    }

    fun dueReminders(
        reminders: List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity>,
        currentMileage: Int?,
        nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        return reminders.map { status(it, currentMileage, nowMillis, zone) }
            .filter { it.isDue }
            .map { it.reminder }
    }

    fun approachingReminders(
        reminders: List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity>,
        currentMileage: Int?,
        nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        return reminders.map { status(it, currentMileage, nowMillis, zone) }
            .filter { it.isApproaching }
            .map { it.reminder }
    }

    @JvmName("approachingRemindersByCar")
    fun approachingReminders(
        reminders: List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity>,
        mileageByCar: Map<String, Int?>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        return reminders.filter { reminder ->
            status(reminder, mileageByCar[reminder.carExternalId], nowMillis, zone).isApproaching
        }
    }

    fun mileageRemindersDue(
        reminders: List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity>,
        currentMileage: Int?,
    ): List<com.mech.carexpensetracker.data.db.entity.CarReminderEntity> {
        return dueReminders(reminders, currentMileage).filter { it.dueMileage != null }
    }
}
