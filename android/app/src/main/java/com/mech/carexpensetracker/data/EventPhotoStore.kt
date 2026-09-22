package com.mech.carexpensetracker.data

import android.content.Context
import android.net.Uri
import java.io.File

object EventPhotoFiles {
    const val DIR = "event_photos"
    const val CAPTURE_DIR = "camera_captures"

    // ponytail: hard cap so a form/export stays small; raise if people actually hit it
    const val MAX_PER_EVENT = 8

    private val knownExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif")

    fun relativePath(photoId: String, extension: String): String {
        val ext = extension.trimStart('.').lowercase().ifBlank { "jpg" }
        val normalized = if (ext == "jpeg") "jpg" else ext
        return "$DIR/$photoId.$normalized"
    }

    fun extensionOf(fileName: String?, mimeType: String?): String {
        val fromName = fileName
            ?.substringAfterLast('/')
            ?.substringAfterLast('.')
            ?.lowercase()
            ?.takeIf { it in knownExtensions }
        if (fromName != null) return if (fromName == "jpeg") "jpg" else fromName
        return when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }

    fun zipEntryName(imagePath: String, photoId: String): String =
        relativePath(photoId, extensionOf(imagePath, null))

    fun findBinary(
        binaries: Map<String, ByteArray>,
        imagePath: String,
        photoId: String,
    ): ByteArray? {
        val normalized = imagePath.replace('\\', '/').trimStart('/')
        val fileName = normalized.substringAfterLast('/')
        val entry = zipEntryName(imagePath, photoId)
        return binaries[normalized]
            ?: binaries[imagePath]
            ?: binaries[entry]
            ?: binaries[fileName]
            ?: binaries["$DIR/$fileName"]
    }

    fun isZipPhotoEntry(entryName: String): Boolean {
        val name = entryName.replace('\\', '/').trimStart('/')
        return name.startsWith("$DIR/") && name != DIR && !name.endsWith('/')
    }

    fun captureFile(cacheDir: File, photoId: String): File {
        val dir = File(cacheDir, CAPTURE_DIR).apply { mkdirs() }
        return File(dir, "$photoId.jpg").apply { createNewFile() }
    }
}

class EventPhotoStore(private val context: Context) {
    fun fileFor(relativePath: String): File {
        val asFile = File(relativePath)
        return if (asFile.isAbsolute) asFile else File(context.filesDir, relativePath)
    }

    fun save(photoId: String, bytes: ByteArray, extension: String): String {
        val relative = EventPhotoFiles.relativePath(photoId, extension)
        val file = fileFor(relative)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return relative
    }

    fun saveFromUri(photoId: String, uri: Uri): String? {
        val mime = context.contentResolver.getType(uri)
        val ext = EventPhotoFiles.extensionOf(uri.lastPathSegment, mime)
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } ?: return null
        if (bytes.isEmpty()) return null
        return save(photoId, bytes, ext)
    }

    fun readBytes(relativePath: String): ByteArray? {
        val file = fileFor(relativePath)
        return if (file.isFile) file.readBytes() else null
    }

    fun delete(relativePath: String) {
        fileFor(relativePath).delete()
    }
}
