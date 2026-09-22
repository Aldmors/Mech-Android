package com.mech.carexpensetracker.import_

import com.mech.carexpensetracker.data.EventPhotoFiles
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class CarnotesZip(
    val tables: Map<String, String>,
    val binaries: Map<String, ByteArray> = emptyMap(),
)

object ImportFileReader {
    fun tablesFromNamedContents(files: List<Pair<String, ByteArray>>): Map<String, String> =
        archiveFromNamedContents(files).tables

    fun archiveFromNamedContents(files: List<Pair<String, ByteArray>>): CarnotesZip {
        val tables = linkedMapOf<String, String>()
        val binaries = linkedMapOf<String, ByteArray>()
        files.forEach { (name, bytes) ->
            if (bytes.isEmpty()) return@forEach
            if (isZip(name, bytes)) {
                val archive = readZip(bytes)
                tables.putAll(archive.tables)
                binaries.putAll(archive.binaries)
            } else {
                val json = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                CarnotesDtos.resolveTableKey(name, json)?.let { tables[it] = json }
            }
        }
        return CarnotesZip(tables, binaries)
    }

    fun tablesFromZip(bytes: ByteArray): Map<String, String> = readZip(bytes).tables

    fun readZip(bytes: ByteArray): CarnotesZip {
        val tables = linkedMapOf<String, String>()
        val binaries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val content = zip.readBytes()
                    val name = entry.name.replace('\\', '/').trimStart('/')
                    if (EventPhotoFiles.isZipPhotoEntry(name)) {
                        binaries[name] = content
                    } else {
                        val json = content.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                        CarnotesDtos.resolveTableKey(name, json)?.let { tables[it] = json }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return CarnotesZip(tables, binaries)
    }

    fun isZip(fileName: String?, bytes: ByteArray): Boolean {
        if (fileName?.substringAfterLast('/')?.endsWith(".zip", ignoreCase = true) == true) return true
        return bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
    }
}
