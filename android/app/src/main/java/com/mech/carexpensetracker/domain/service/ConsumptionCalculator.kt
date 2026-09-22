package com.mech.carexpensetracker.domain.service

import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.model.FuelSlot
import com.mech.carexpensetracker.domain.model.VehicleUnits
import java.math.BigDecimal
import java.math.RoundingMode

object ConsumptionCalculator {
    private val litersPerUsGallon = BigDecimal("3.785411784")

    fun litersPer100Km(amountLiters: BigDecimal, distanceKm: Int): BigDecimal? {
        if (distanceKm <= 0) return null
        return amountLiters.multiply(BigDecimal(100)).divide(BigDecimal(distanceKm), 2, RoundingMode.HALF_UP)
    }

    fun mpg(usGallons: BigDecimal, distanceMi: Int): BigDecimal? {
        if (usGallons <= BigDecimal.ZERO || distanceMi <= 0) return null
        return BigDecimal(distanceMi).divide(usGallons, 2, RoundingMode.HALF_UP)
    }

    fun consumptionBetweenEvents(
        previousMileage: Int?,
        currentMileage: Int?,
        fuelAmount: BigDecimal?,
        units: VehicleUnits,
    ): BigDecimal? {
        if (previousMileage == null || currentMileage == null || fuelAmount == null) return null
        val distance = currentMileage - previousMileage
        if (distance <= 0 || fuelAmount.signum() <= 0) return null
        return consumptionForDistance(fuelAmount, distance, units)
    }

    fun fuelAmount(event: CarEventEntity, slot: FuelSlot = FuelSlot.Primary): BigDecimal? =
        CurrencyFormatter.parseStored(
            when (slot) {
                FuelSlot.Primary -> event.fuelAmount
                FuelSlot.Secondary -> event.secondaryFuelAmount
            },
        )

    fun isFullTank(event: CarEventEntity, slot: FuelSlot): Boolean = when (slot) {
        FuelSlot.Primary -> event.fuelFullTank
        FuelSlot.Secondary -> event.secondaryFuelFullTank
    }

    fun consumptionByEventId(
        events: List<CarEventEntity>,
        units: VehicleUnits,
        slot: FuelSlot = FuelSlot.Primary,
    ): Map<String, BigDecimal> = buildMap {
        fullToFullFills(events, slot).forEach { fill ->
            consumptionForDistance(fill.amount, fill.distance, units)
                ?.let { put(fill.eventId, it) }
        }
    }

    fun displayedConsumptionByEventId(
        events: List<CarEventEntity>,
        units: VehicleUnits,
    ): Map<String, BigDecimal> {
        val secondary = consumptionByEventId(events, units, FuelSlot.Secondary)
        return secondary + consumptionByEventId(events, units, FuelSlot.Primary)
    }

    fun averageConsumption(
        events: List<CarEventEntity>,
        units: VehicleUnits,
        slot: FuelSlot = FuelSlot.Primary,
    ): BigDecimal? {
        val fills = fullToFullFills(events, slot)
        if (fills.isEmpty()) return null
        val amount = fills.fold(BigDecimal.ZERO) { acc, fill -> acc + fill.amount }
        val distance = fills.sumOf { it.distance }
        return consumptionForDistance(amount, distance, units)
    }

    fun previousFuelEvents(events: List<CarEventEntity>): Map<String, CarEventEntity> {
        val fuelEvents = chronologicalFuel(events)
        if (fuelEvents.size < 2) return emptyMap()
        return buildMap {
            for (index in 1 until fuelEvents.size) {
                put(fuelEvents[index].externalId, fuelEvents[index - 1])
            }
        }
    }

    private data class FullToFullFill(
        val eventId: String,
        val amount: BigDecimal,
        val distance: Int,
    )

    private fun fullToFullFills(
        events: List<CarEventEntity>,
        slot: FuelSlot,
    ): List<FullToFullFill> {
        val fuelEvents = chronologicalFuel(events).filter { fuelAmount(it, slot) != null }
        if (fuelEvents.size < 2) return emptyList()
        return buildList {
            fuelEvents.groupBy { fuelTypeKey(it, slot) }.values.forEach { group ->
                if (group.size < 2) return@forEach
                group.forEachIndexed { index, event ->
                    if (!isFullTank(event, slot)) return@forEachIndexed
                    val previousFullIndex = (index - 1 downTo 0).firstOrNull { isFullTank(group[it], slot) }
                        ?: return@forEachIndexed
                    val previous = group[previousFullIndex]
                    val distance = (event.mileage ?: return@forEachIndexed) -
                        (previous.mileage ?: return@forEachIndexed)
                    if (distance <= 0) return@forEachIndexed
                    val filled = group.subList(previousFullIndex + 1, index + 1)
                        .mapNotNull { fuelAmount(it, slot) }
                        .fold(BigDecimal.ZERO) { acc, value -> acc + value }
                    if (filled.signum() <= 0) return@forEachIndexed
                    add(FullToFullFill(event.externalId, filled, distance))
                }
            }
        }
    }

    private fun consumptionForDistance(
        amount: BigDecimal,
        distance: Int,
        units: VehicleUnits,
    ): BigDecimal? = when (units) {
        VehicleUnits.Km -> litersPer100Km(amount, distance)
        VehicleUnits.Mi -> mpg(
            amount.divide(litersPerUsGallon, 10, RoundingMode.HALF_UP),
            distance,
        )
    }

    private fun fuelTypeKey(event: CarEventEntity, slot: FuelSlot): String =
        when (slot) {
            FuelSlot.Primary -> event.fuelTypeRaw
            FuelSlot.Secondary -> event.secondaryFuelTypeRaw
        }?.trim()?.lowercase().orEmpty()

    private fun chronologicalFuel(events: List<CarEventEntity>): List<CarEventEntity> =
        events
            .filter { EventType.fromRaw(it.typeRaw) == EventType.Fuel }
            .sortedWith(compareBy({ it.dateMillis }, { it.id }))
}
