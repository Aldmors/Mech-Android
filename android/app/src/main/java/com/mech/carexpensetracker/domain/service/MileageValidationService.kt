package com.mech.carexpensetracker.domain.service

import com.mech.carexpensetracker.data.db.entity.CarEventEntity

data class MileageValidationResult(
    val isValid: Boolean,
    val warning: String? = null,
)

object MileageValidationService {
    fun validate(
        mileage: Int?,
        previousEvents: List<CarEventEntity>,
        excludeExternalId: String? = null,
    ): MileageValidationResult {
        if (mileage == null) return MileageValidationResult(isValid = true)
        val filtered = previousEvents.filter { it.externalId != excludeExternalId }
        val maxMileage = filtered.mapNotNull { it.mileage }.maxOrNull()
        if (maxMileage != null && mileage < maxMileage) {
            return MileageValidationResult(
                isValid = false,
                warning = "Mileage ($mileage) is lower than the latest recorded ($maxMileage).",
            )
        }
        return MileageValidationResult(isValid = true)
    }
}
