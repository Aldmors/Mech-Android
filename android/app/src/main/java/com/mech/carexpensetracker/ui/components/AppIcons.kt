package com.mech.carexpensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import com.mech.carexpensetracker.domain.model.EventType

object AppIcons {
    fun eventType(type: EventType, fullTank: Boolean = false): ImageVector = when (type) {
        EventType.Fuel -> if (fullTank) Icons.Default.Speed else Icons.Default.LocalGasStation
        EventType.Repair -> Icons.Default.Build
        EventType.Papers -> Icons.Default.Description
    }

    val eventsTab: ImageVector = Icons.AutoMirrored.Filled.List
}
