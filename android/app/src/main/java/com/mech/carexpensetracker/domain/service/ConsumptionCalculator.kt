package com.mech.carexpensetracker.domain.service

import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.model.FuelType
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.model.fuelLegsFromEvent
import java.math.BigDecimal
import java.math.RoundingMode

object ConsumptionCalculator {
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
        if (distance <= 0) return null
        return when (units) {
            VehicleUnits.Km -> litersPer100Km(fuelAmount, distance)
            VehicleUnits.Mi -> mpg(fuelAmount, distance)
        }
    }

    fun consumptionForEvent(
        event: CarEventEntity,
        previousEvent: CarEventEntity?,
        units: VehicleUnits,
    ): BigDecimal? {
        val legs = fuelLegsFromEvent(
            FuelType.fromRaw(event.fuelTypeRaw),
            event.fuelAmount?.toBigDecimalOrNull(),
            event.fuelCost?.toBigDecimalOrNull(),
            event.fuelFullTank,
            FuelType.fromRaw(event.secondaryFuelTypeRaw),
            event.secondaryFuelAmount?.toBigDecimalOrNull(),
            event.secondaryFuelCost?.toBigDecimalOrNull(),
            event.secondaryFuelFullTank,
        )
        val primaryAmount = legs.firstOrNull()?.amount ?: return null
        return consumptionBetweenEvents(
            previousEvent?.mileage,
            event.mileage,
            primaryAmount,
            units,
        )
    }

    fun averageConsumption(
        events: List<CarEventEntity>,
        units: VehicleUnits,
    ): BigDecimal? {
        val fuelEvents = events
            .filter { EventType.fromRaw(it.typeRaw) == EventType.Fuel }
            .sortedBy { it.dateMillis }
        val values = fuelEvents.mapIndexedNotNull { index, event ->
            val previous = fuelEvents.getOrNull(index - 1)
            consumptionForEvent(event, previous, units)
        }
        if (values.isEmpty()) return null
        return values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
            .divide(BigDecimal(values.size), 2, RoundingMode.HALF_UP)
    }
}
