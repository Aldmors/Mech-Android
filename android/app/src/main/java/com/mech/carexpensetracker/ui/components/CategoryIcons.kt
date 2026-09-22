package com.mech.carexpensetracker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun parseCategoryColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color(0xFF673AB7)
}

fun hsvHex(hue: Float): String {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.72f, 0.86f))
    return "#%06X".format(0xFFFFFF and color)
}

fun hexToHue(hex: String): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(parseCategoryColor(hex).toArgb(), hsv)
    return hsv[0]
}
