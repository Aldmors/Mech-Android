package com.mech.carexpensetracker.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CarIconTest {
    @Test
    fun unknownNameFallsBackToCar() {
        assertEquals(CarIcon.DEFAULT, CarIcon.resolve(null))
        assertEquals(CarIcon.DEFAULT, CarIcon.resolve(""))
        assertEquals(CarIcon.DEFAULT, CarIcon.resolve("not-an-icon"))
        assertEquals("ElectricCar", CarIcon.resolve("ElectricCar"))
        assertEquals("ElectricCar", CarIcon.resolve("electriccar"))
    }
}
