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
        val care: BigDecimal,
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
            var care = BigDecimal.ZERO
            monthEvents.forEach { event ->
                val cost = CurrencyFormatter.parseStored(event.totalCost) ?: BigDecimal.ZERO
                when (EventType.fromRaw(event.typeRaw)) {
                    EventType.Fuel -> fuel += cost
                    EventType.Repair -> repair += cost
                    EventType.Papers -> papers += cost
                    EventType.Care -> care += cost
                }
            }
            MonthlySpendingBar(key, fuel, repair, papers, care)
        }
    }

    fun fuelConsumptionPoints(
        events: List<CarEventEntity>,
        units: com.mech.carexpensetracker.domain.model.VehicleUnits,
        visibleEvents: List<CarEventEntity> = events,
    ): List<ConsumptionPoint> {
        val byId = ConsumptionCalculator.displayedConsumptionByEventId(events, units)
        val visibleIds = visibleEvents.map { it.externalId }.toSet()
        return events
            .filter { EventType.fromRaw(it.typeRaw) == EventType.Fuel && it.externalId in visibleIds }
            .sortedBy { it.dateMillis }
            .mapNotNull { event ->
                val value = byId[event.externalId] ?: return@mapNotNull null
                val label = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                ConsumptionPoint(label, value)
            }
    }

    fun categoryBreakdown(events: List<CarEventEntity>): List<CategorySlice> {
        val amounts = EventType.entries.associateWith { BigDecimal.ZERO }.toMutableMap()
        events.forEach { event ->
            val cost = CurrencyFormatter.parseStored(event.totalCost) ?: BigDecimal.ZERO
            val type = EventType.fromRaw(event.typeRaw)
            amounts[type] = amounts.getValue(type) + cost
        }
        val slices = EventType.entries.map { type ->
            CategorySlice(eventTypeLabel(type), amounts.getValue(type))
        }
        return if (slices.any { it.amount.signum() > 0 }) slices else emptyList()
    }

    private fun eventTypeLabel(type: EventType): String = when (type) {
        EventType.Fuel -> "Paliwo"
        EventType.Repair -> "Serwis"
        EventType.Papers -> "Dokumenty"
        EventType.Care -> "Pielęgnacja"
    }

    fun cumulativeCost(events: List<CarEventEntity>): List<CumulativePoint> {
        val sorted = events.sortedBy { it.dateMillis }
        var running = BigDecimal.ZERO
        return sorted.map { event ->
            running += CurrencyFormatter.parseStored(event.totalCost) ?: BigDecimal.ZERO
            val label = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            CumulativePoint(label, running)
        }
    }
}
