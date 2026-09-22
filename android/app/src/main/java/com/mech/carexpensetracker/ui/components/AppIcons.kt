package com.mech.carexpensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ElectricMoped
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Moped
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.RvHookup
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TimeToLeave
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import com.mech.carexpensetracker.domain.model.CarIcon
import com.mech.carexpensetracker.domain.model.EventType

object AppIcons {
    fun eventType(type: EventType, fullTank: Boolean = false): ImageVector = when (type) {
        EventType.Fuel -> if (fullTank) Icons.Default.Speed else Icons.Default.LocalGasStation
        EventType.Repair -> Icons.Default.Build
        EventType.Papers -> Icons.Default.Description
        EventType.Care -> Icons.Default.LocalCarWash
    }

    val eventsTab: ImageVector = Icons.AutoMirrored.Outlined.List
    val eventsTabSelected: ImageVector = Icons.AutoMirrored.Filled.List

    fun car(name: String?): ImageVector = when (CarIcon.resolve(name)) {
        "ElectricCar" -> Icons.Default.ElectricCar
        "CarRental" -> Icons.Default.CarRental
        "CarRepair" -> Icons.Default.CarRepair
        "LocalTaxi" -> Icons.Default.LocalTaxi
        "LocalShipping" -> Icons.Default.LocalShipping
        "AirportShuttle" -> Icons.Default.AirportShuttle
        "TwoWheeler" -> Icons.Default.TwoWheeler
        "PedalBike" -> Icons.Default.PedalBike
        "Moped" -> Icons.Default.Moped
        "ElectricMoped" -> Icons.Default.ElectricMoped
        "DirectionsBus" -> Icons.Default.DirectionsBus
        "DirectionsBoat" -> Icons.Default.DirectionsBoat
        "Agriculture" -> Icons.Default.Agriculture
        "RvHookup" -> Icons.Default.RvHookup
        "Commute" -> Icons.Default.Commute
        "Garage" -> Icons.Default.Garage
        "TimeToLeave" -> Icons.Default.TimeToLeave
        "LocalCarWash" -> Icons.Default.LocalCarWash
        "LocalGasStation" -> Icons.Default.LocalGasStation
        "EvStation" -> Icons.Default.EvStation
        "Speed" -> Icons.Default.Speed
        "Traffic" -> Icons.Default.Traffic
        else -> Icons.Default.DirectionsCar
    }
}
