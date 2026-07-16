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
    val assetUpdateAvailable: Boolean,
)
