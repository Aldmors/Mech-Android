package com.mech.carexpensetracker.domain.service

import com.mech.carexpensetracker.data.db.entity.CarEventEntity

data class MileageValidationResult(
    val isValid: Boolean,
    val minAllowed: Int? = null,
)

object MileageValidationService {
    fun validate(
        mileage: Int?,
        previousEvents: List<CarEventEntity>,
        excludeExternalId: String? = null,
        dateMillis: Long = Long.MAX_VALUE,
    ): MileageValidationResult {
        if (mileage == null) return MileageValidationResult(isValid = true)
        if (mileage < 0) {
            return MileageValidationResult(isValid = false)
        }
        val others = previousEvents.filter { it.externalId != excludeExternalId }
        val minAllowed = minAllowedMileage(others, dateMillis)
        if (minAllowed != null && mileage < minAllowed) {
            return MileageValidationResult(isValid = false, minAllowed = minAllowed)
        }
        return MileageValidationResult(isValid = true)
    }

    private fun minAllowedMileage(
        others: List<CarEventEntity>,
        dateMillis: Long,
    ): Int? {
        val dated = others.filter { it.mileage != null }
        if (dated.isEmpty()) return null
        val isNewest = dated.none { it.dateMillis > dateMillis }
        if (isNewest) {
            return dated.mapNotNull { it.mileage }.maxOrNull()
        }
        return dated
            .filter { it.dateMillis < dateMillis }
            .maxWithOrNull(compareBy<CarEventEntity> { it.dateMillis }.thenBy { it.externalId })
            ?.mileage
    }
}
