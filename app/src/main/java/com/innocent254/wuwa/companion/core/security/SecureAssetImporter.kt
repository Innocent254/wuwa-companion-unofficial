package com.innocent254.wuwa.companion.core.security

import java.io.File
import java.util.zip.ZipInputStream

class SecureAssetImporter(
    private val store: SecureAssetStore,
) {
    fun importPackage(packageFile: File): Int {
        var imported = 0
        var totalExpandedBytes = 0L

        packageFile.inputStream().buffered().use { fileInput ->
            ZipInputStream(fileInput).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue

                    val normalized = entry.name.replace('\\', '/')
                    require(!normalized.startsWith("/")) { "Absolute ZIP path rejected." }
                    require(normalized.split('/').none { it == ".." }) {
                        "ZIP traversal path rejected."
                    }
                    require(entry.size <= MAX_SINGLE_ASSET_BYTES || entry.size == -1L) {
                        "Asset is larger than the configured limit."
                    }

                    val limited = LimitedInputStream(zip, MAX_SINGLE_ASSET_BYTES)
                    store.put(assetId = normalized, source = limited)
                    totalExpandedBytes += limited.bytesRead
                    require(totalExpandedBytes <= MAX_TOTAL_EXPANDED_BYTES) {
                        "Expanded asset package is larger than the configured limit."
                    }

                    imported += 1
                    zip.closeEntry()
                }
            }
        }

        packageFile.delete()
        return imported
    }

    private class LimitedInputStream(
        private val delegate: java.io.InputStream,
        private val limit: Long,
    ) : java.io.InputStream() {
        var bytesRead: Long = 0
            private set

        override fun read(): Int {
            val value = delegate.read()
            if (value >= 0) account(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val remaining = limit - bytesRead
            require(remaining > 0) { "Asset expanded beyond its limit." }
            val count = delegate.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (count > 0) account(count.toLong())
            return count
        }

        private fun account(count: Long) {
            bytesRead += count
            require(bytesRead <= limit) { "Asset expanded beyond its limit." }
        }
    }

    private companion object {
        const val MAX_SINGLE_ASSET_BYTES = 25L * 1024L * 1024L
        const val MAX_TOTAL_EXPANDED_BYTES = 750L * 1024L * 1024L
    }
}
