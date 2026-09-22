package com.mech.carexpensetracker.ui.charts

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Window
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

internal object ChartImageSaver {
    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    suspend fun capture(window: Window, bounds: Rect): Bitmap? {
        val view = window.decorView
        val region = clampedCaptureRect(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            viewWidth = view.width,
            viewHeight = view.height,
        ) ?: return null
        return suspendCancellableCoroutine { continuation ->
            val bitmap = Bitmap.createBitmap(region.width, region.height, Bitmap.Config.ARGB_8888)
            val src = android.graphics.Rect(region.left, region.top, region.right, region.bottom)
            PixelCopy.request(
                window,
                src,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        continuation.resume(bitmap)
                    } else {
                        bitmap.recycle()
                        continuation.resume(null)
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
    }

    fun savePng(context: Context, bitmap: Bitmap): Boolean {
        val resolver = context.contentResolver
        val name = "wykres_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            val written = resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
            if (!written) {
                resolver.delete(uri, null, null)
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}

internal data class CaptureRegion(val left: Int, val top: Int, val width: Int, val height: Int) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
}

internal fun clampedCaptureRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    viewWidth: Int,
    viewHeight: Int,
): CaptureRegion? {
    val clampedLeft = left.toInt().coerceIn(0, viewWidth)
    val clampedTop = top.toInt().coerceIn(0, viewHeight)
    val clampedRight = right.toInt().coerceIn(clampedLeft, viewWidth)
    val clampedBottom = bottom.toInt().coerceIn(clampedTop, viewHeight)
    val width = clampedRight - clampedLeft
    val height = clampedBottom - clampedTop
    if (width <= 0 || height <= 0) return null
    return CaptureRegion(clampedLeft, clampedTop, width, height)
}
