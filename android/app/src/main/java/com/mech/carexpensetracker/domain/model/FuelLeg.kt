package com.mech.carexpensetracker.domain.model

import java.math.BigDecimal
import java.time.Instant

data class FuelLeg(
    val fuelType: FuelType,
    val amount: BigDecimal?,
    val cost: BigDecimal?,
    val isFullTank: Boolean,
)

data class CarEventWithLegs(
    val externalId: String,
    val carExternalId: String,
    val type: EventType,
    val date: Instant,
    val mileage: Int?,
    val comment: String?,
    val totalCost: BigDecimal?,
    val categoryName: String?,
    val partsCost: BigDecimal?,
    val labourCost: BigDecimal?,
    val primaryLeg: FuelLeg?,
    val secondaryLeg: FuelLeg?,
)

fun fuelLegsFromEvent(
    primaryFuelType: FuelType?,
    fuelAmount: BigDecimal?,
    fuelCost: BigDecimal?,
    fuelFullTank: Boolean,
    secondaryFuelType: FuelType?,
    secondaryFuelAmount: BigDecimal?,
    secondaryFuelCost: BigDecimal?,
    secondaryFuelFullTank: Boolean,
): List<FuelLeg> {
    val legs = mutableListOf<FuelLeg>()
    if (primaryFuelType != null && (fuelAmount != null || fuelCost != null)) {
        legs += FuelLeg(primaryFuelType, fuelAmount, fuelCost, fuelFullTank)
    }
    if (secondaryFuelType != null && (secondaryFuelAmount != null || secondaryFuelCost != null)) {
        legs += FuelLeg(secondaryFuelType, secondaryFuelAmount, secondaryFuelCost, secondaryFuelFullTank)
    }
    return legs
}
