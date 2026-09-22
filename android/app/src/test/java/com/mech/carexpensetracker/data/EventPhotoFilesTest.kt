package com.mech.carexpensetracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class EventPhotoFilesTest {
    @Test
    fun relativePathNormalizesJpeg() {
        assertEquals("event_photos/abc.jpg", EventPhotoFiles.relativePath("abc", "jpeg"))
        assertEquals("event_photos/abc.png", EventPhotoFiles.relativePath("abc", ".png"))
    }

    @Test
    fun extensionPrefersFileNameThenMime() {
        assertEquals("jpg", EventPhotoFiles.extensionOf("receipt.JPEG", "image/png"))
        assertEquals("png", EventPhotoFiles.extensionOf("no-ext", "image/png"))
        assertEquals("jpg", EventPhotoFiles.extensionOf(null, null))
    }

    @Test
    fun zipHelpersKeepPhotosUnderEventPhotosDir() {
        assertEquals("event_photos/p-1.jpg", EventPhotoFiles.zipEntryName("/tmp/event_photos/old.jpg", "p-1"))
        assertTrue(EventPhotoFiles.isZipPhotoEntry("event_photos/p-1.jpg"))
        assertFalse(EventPhotoFiles.isZipPhotoEntry("event_photos_table.json"))
        assertFalse(EventPhotoFiles.isZipPhotoEntry("garage_table.json"))
    }

    @Test
    fun findBinaryAcceptsLegacyAndCanonicalKeys() {
        val bytes = byteArrayOf(1, 2, 3)
        val binaries = mapOf("event_photos/p-1.jpg" to bytes)
        assertTrue(
            EventPhotoFiles.findBinary(binaries, "p-1.jpg", "p-1")?.contentEquals(bytes) == true,
        )
        assertTrue(
            EventPhotoFiles.findBinary(binaries, "event_photos/p-1.jpg", "p-1")?.contentEquals(bytes) == true,
        )
        assertNull(EventPhotoFiles.findBinary(binaries, "missing.png", "missing"))
    }

    @Test
    fun captureFileCreatesJpegUnderCameraCaptures() {
        val cache = File(System.getProperty("java.io.tmpdir"), "event-photo-capture-${System.nanoTime()}")
        try {
            val file = EventPhotoFiles.captureFile(cache, "abc")
            assertEquals(File(cache, "camera_captures/abc.jpg").canonicalFile, file.canonicalFile)
            assertTrue(file.isFile)
        } finally {
            cache.deleteRecursively()
        }
    }
}
