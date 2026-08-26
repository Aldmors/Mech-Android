package com.mech.carexpensetracker.domain.service

import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.domain.model.ChartDatePreset
import com.mech.carexpensetracker.domain.model.EventType
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ChartDataService {
    data class MonthlySpendingBar(
        val label: String,
        val fuel: BigDecimal,
        val repair: BigDecimal,
        val papers: BigDecimal,
    )

    data class ConsumptionPoint(
        val label: String,
        val value: BigDecimal,
    )

    data class CategorySlice(
        val name: String,
        val amount: BigDecimal,
    )

    data class CumulativePoint(
        val label: String,
        val total: BigDecimal,
    )

    fun filterByPreset(
        events: List<CarEventEntity>,
        preset: ChartDatePreset,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<CarEventEntity> {
        if (preset.months == null) return events
        val cutoff = Instant.now().minus(preset.months.toLong() * 30, ChronoUnit.DAYS)
        return events.filter { Instant.ofEpochMilli(it.dateMillis).isAfter(cutoff) }
    }

    fun monthlySpending(events: List<CarEventEntity>): List<MonthlySpendingBar> {
        val grouped = events.groupBy { event ->
            val zdt = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneId.systemDefault())
            "${zdt.year}-${zdt.monthValue.toString().padStart(2, '0')}"
        }
        return grouped.entries.sortedBy { it.key }.map { (key, monthEvents) ->
            var fuel = BigDecimal.ZERO
            var repair = BigDecimal.ZERO
            var papers = BigDecimal.ZERO
            monthEvents.forEach { event ->
                val cost = event.totalCost?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                when (EventType.fromRaw(event.typeRaw)) {
                    EventType.Fuel -> fuel += cost
                    EventType.Repair -> repair += cost
                    EventType.Papers -> papers += cost
                }
            }
            MonthlySpendingBar(key, fuel, repair, papers)
        }
    }

    fun fuelConsumptionPoints(
        events: List<CarEventEntity>,
        units: com.mech.carexpensetracker.domain.model.VehicleUnits,
    ): List<ConsumptionPoint> {
        val fuelEvents = events
            .filter { EventType.fromRaw(it.typeRaw) == EventType.Fuel }
            .sortedBy { it.dateMillis }
        return fuelEvents.mapIndexedNotNull { index, event ->
            val previous = fuelEvents.getOrNull(index - 1)
            val value = ConsumptionCalculator.consumptionForEvent(event, previous, units) ?: return@mapIndexedNotNull null
            val label = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            ConsumptionPoint(label, value)
        }
    }

    fun categoryBreakdown(events: List<CarEventEntity>): List<CategorySlice> {
        val repairEvents = events.filter { EventType.fromRaw(it.typeRaw) == EventType.Repair }
        val grouped = repairEvents.groupBy { it.categoryName ?: "Other" }
        return grouped.map { (name, items) ->
            val amount = items.mapNotNull { it.totalCost?.toBigDecimalOrNull() }
                .fold(BigDecimal.ZERO) { acc, v -> acc + v }
            CategorySlice(name, amount)
        }.filter { it.amount > BigDecimal.ZERO }
    }

    fun cumulativeCost(events: List<CarEventEntity>): List<CumulativePoint> {
        val sorted = events.sortedBy { it.dateMillis }
        var running = BigDecimal.ZERO
        return sorted.map { event ->
            running += event.totalCost?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val label = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            CumulativePoint(label, running)
        }
    }
}
