package com.innocent254.wuwa.companion.core.update

import android.content.Context
import com.innocent254.wuwa.companion.BuildConfig
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    suspend fun check(
        currentAppVersionCode: Int,
        currentDatabaseVersion: String,
        currentAssetVersion: String,
    ): UpdateAvailability = withContext(Dispatchers.IO) {
        val appManifest = runCatching {
            fetch<AppUpdateManifest>(BuildConfig.APP_MANIFEST_URL)
        }.getOrNull()

        val dataManifest = runCatching {
            fetch<DataUpdateManifest>(BuildConfig.DATABASE_MANIFEST_URL)
        }.getOrNull()

        UpdateAvailability(
            app = appManifest,
            data = dataManifest,
            appUpdateAvailable = appManifest?.let {
                it.available && it.versionCode > currentAppVersionCode
            } == true,
            databaseUpdateAvailable = dataManifest?.database?.let {
                it.available && isNewer(it.version, currentDatabaseVersion)
            } == true,
            assetUpdateAvailable = dataManifest?.assets?.let {
                it.available && isNewer(it.version, currentAssetVersion)
            } == true,
        )
    }

    suspend fun downloadVerified(
        url: String,
        expectedSha256: String,
        fileName: String,
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.noBackupFilesDir, "update_staging").apply { mkdirs() }
        val staging = File(directory, "$fileName.part")
        val target = File(directory, fileName)

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Download failed with HTTP ${response.code}" }
            val body = checkNotNull(response.body) { "Response body was empty." }
            staging.outputStream().use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
        }

        check(sha256(staging).equals(expectedSha256, ignoreCase = true)) {
            staging.delete()
            "Downloaded update failed SHA-256 verification."
        }

        if (target.exists()) target.delete()
        check(staging.renameTo(target)) { "Could not finalize update download." }
        target
    }

    private inline fun <reified T> fetch(url: String): T {
        val request = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Manifest request failed with HTTP ${response.code}" }
            val content = checkNotNull(response.body).string()
            return json.decodeFromString(content)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(remoteParts.size, localParts.size)) { index ->
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val localPart = localParts.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }
}
