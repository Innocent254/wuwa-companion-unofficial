package com.wuwa.companion.core.update

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.security.MessageDigest

/**
 * Coordinates the full "is there a new database?" -> "download" ->
 * "verify" -> "migrate" -> "swap or rollback" lifecycle.
 *
 * Called from a periodic WorkManager job (UpdateCheckWorker) and also
 * on-demand from Settings ("Check for updates now").
 */
class UpdateManager(
    private val api: UpdateApi,
    private val fileStore: UpdateFileStore,
    private val localVersionStore: LocalVersionStore,
    private val databaseMigrator: DatabaseMigrator,
) {

    sealed class UpdateState {
        data object Checking : UpdateState()
        data object UpToDate : UpdateState()
        data class UpdateAvailable(val manifest: VersionManifest) : UpdateState()
        data class Downloading(val progress: Float) : UpdateState()
        data object Verifying : UpdateState()
        data object Migrating : UpdateState()
        data object Success : UpdateState()
        data class Failed(val reason: String, val rolledBack: Boolean) : UpdateState()
    }

    /**
     * Full check-and-apply flow. Emits progress states so the UI (a
     * Settings screen or a silent background notification) can react.
     */
    fun checkAndApplyUpdate(): Flow<UpdateState> = flow {
        emit(UpdateState.Checking)

        val manifest = try {
            api.fetchVersionManifest()
        } catch (e: Exception) {
            emit(UpdateState.Failed("Could not reach update server: ${e.message}", rolledBack = false))
            return@flow
        }

        val localVersion = localVersionStore.getCurrentDatabaseVersion()
        if (!isNewer(manifest.database.version, localVersion)) {
            emit(UpdateState.UpToDate)
            return@flow
        }

        emit(UpdateState.UpdateAvailable(manifest))

        // Prefer a delta package from our current version if the manifest offers one —
        // falls back to the full package otherwise (e.g. first install, or too many
        // versions behind for a delta chain to exist).
        val delta = manifest.database.deltaFrom[localVersion]
        val downloadUrl = delta?.url ?: manifest.database.fullPackageUrl
        val expectedSha256 = delta?.sha256 ?: manifest.database.fullPackageSha256
        val isDelta = delta != null

        val downloadedFile: File
        try {
            downloadedFile = fileStore.downloadResumable(
                url = downloadUrl,
                onProgress = { /* progress plumbed to a StateFlow the UI collects */ },
            )
        } catch (e: Exception) {
            emit(UpdateState.Failed("Download failed: ${e.message}", rolledBack = false))
            return@flow
        }

        emit(UpdateState.Verifying)
        if (!verifyChecksum(downloadedFile, expectedSha256)) {
            downloadedFile.delete()
            emit(UpdateState.Failed("Checksum mismatch — package rejected, nothing applied.", rolledBack = false))
            return@flow
        }

        emit(UpdateState.Migrating)
        val migrationResult = try {
            if (isDelta) {
                databaseMigrator.applyDelta(downloadedFile)
            } else {
                databaseMigrator.applyFull(downloadedFile)
            }
        } catch (e: Exception) {
            // applyDelta/applyFull are expected to leave the ACTIVE db untouched
            // until a final atomic rename — so a thrown exception here means the
            // staged version is scrapped, not that live data is corrupted.
            emit(UpdateState.Failed("Migration failed: ${e.message}", rolledBack = true))
            return@flow
        }

        if (!migrationResult.success) {
            emit(UpdateState.Failed(migrationResult.reason ?: "Unknown migration failure", rolledBack = true))
            return@flow
        }

        localVersionStore.setCurrentDatabaseVersion(manifest.database.version)
        fileStore.markPreviousDbForOneCycleRetention() // enables rollback on next-boot canary check
        emit(UpdateState.Success)
    }

    /**
     * Called once at app startup. If the previous session crashed
     * immediately after a DB swap (a "boot canary" flag not cleared),
     * roll back to the retained previous database automatically.
     */
    suspend fun performBootCanaryCheck() {
        if (localVersionStore.didLastSessionCrashAfterUpdate()) {
            databaseMigrator.rollbackToPrevious()
            localVersionStore.clearCrashFlag()
        }
    }

    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }

    private fun isNewer(remote: String, local: String): Boolean {
        // Simple semver comparison (major.minor.patch). Swap for a proper
        // semver library if pre-release tags ever need to be supported.
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}

/** DTOs mirroring database-schemas/version.schema.json — keep in sync. */
data class VersionManifest(
    val manifestVersion: Int,
    val database: DatabaseInfo,
    val changelogUrl: String,
    val minSupportedAppVersion: String,
) {
    data class DatabaseInfo(
        val version: String,
        val fullPackageUrl: String,
        val fullPackageSha256: String,
        val deltaFrom: Map<String, DeltaInfo>,
    )
    data class DeltaInfo(val url: String, val sha256: String)
}

interface UpdateApi {
    suspend fun fetchVersionManifest(): VersionManifest
}

interface UpdateFileStore {
    suspend fun downloadResumable(url: String, onProgress: (Float) -> Unit): File
    fun markPreviousDbForOneCycleRetention()
}

interface LocalVersionStore {
    fun getCurrentDatabaseVersion(): String
    fun setCurrentDatabaseVersion(version: String)
    fun didLastSessionCrashAfterUpdate(): Boolean
    fun clearCrashFlag()
}

interface DatabaseMigrator {
    suspend fun applyFull(packageFile: File): MigrationResult
    suspend fun applyDelta(packageFile: File): MigrationResult
    suspend fun rollbackToPrevious()
}

data class MigrationResult(val success: Boolean, val reason: String? = null)
