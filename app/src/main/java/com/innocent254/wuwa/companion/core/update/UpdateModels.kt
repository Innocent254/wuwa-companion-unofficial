package com.innocent254.wuwa.companion.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataUpdateManifest(
    @SerialName("manifest_version") val manifestVersion: Int,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("game_patch") val gamePatch: String? = null,
    val database: PackageInfo,
    val assets: PackageInfo,
    @SerialName("changelog_url") val changelogUrl: String? = null,
    @SerialName("minimum_app_version_code") val minimumAppVersionCode: Int,
    @SerialName("source_summary") val sourceSummary: Map<String, Int> = emptyMap(),
)

@Serializable
data class PackageInfo(
    val version: String,
    val available: Boolean,
    val url: String? = null,
    val sha256: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)

@Serializable
data class AppUpdateManifest(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("minimum_android_sdk") val minimumAndroidSdk: Int,
    @SerialName("supports_images") val supportsImages: Boolean? = null,
    @SerialName("apk_url") val apkUrl: String? = null,
    @SerialName("apk_sha256") val apkSha256: String? = null,
    @SerialName("release_notes_url") val releaseNotesUrl: String? = null,
    val available: Boolean = false,
)

data class UpdateAvailability(
    val app: AppUpdateManifest?,
    val data: DataUpdateManifest?,
    val appUpdateAvailable: Boolean,
    val databaseUpdateAvailable: Boolean,
    val databaseRequiresAppUpdate: Boolean,
    val assetUpdateAvailable: Boolean,
)

enum class UpdatePhase {
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    NOT_AVAILABLE,
    ERROR,
}

data class UpdateItemUiState(
    val installedVersion: String,
    val availableVersion: String? = null,
    val phase: UpdatePhase = UpdatePhase.CHECKING,
    val progress: Float = 0f,
    val message: String? = null,
    val downloadedFilePath: String? = null,
    val justUpdated: Boolean = false,
)

data class UpdateCenterUiState(
    val database: UpdateItemUiState,
    val app: UpdateItemUiState,
    val assetVersion: String,
    val supportsImages: Boolean,
    /**
     * null while the remote data manifest is still being checked,
     * true only when this release publishes a usable image package.
     */
    val imagePackageAvailable: Boolean? = null,
)

sealed interface UpdateEvent {
    data class ToastMessage(val text: String) : UpdateEvent
    data class InstallApk(val absolutePath: String) : UpdateEvent
}
