package com.innocent254.wuwa.companion.ui.preferences

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.innocent254.wuwa.companion.BuildConfig

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Compatibility type for the previous UI foundation. Image behavior is now
 * fixed by BuildConfig.SUPPORTS_IMAGES and is no longer user-selectable.
 */
enum class ImageMode {
    AUTO,
    SHOW,
    HIDE,
}

class UiPreferences(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    private val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    private val isFreshInstall = packageInfo.firstInstallTime == packageInfo.lastUpdateTime

    private var _themeMode by mutableStateOf(
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
    )

    val themeMode: ThemeMode
        get() = _themeMode

    private var _onboardingComplete by mutableStateOf(
        preferences.getBoolean(KEY_ONBOARDING_COMPLETE, !isFreshInstall),
    )

    val onboardingComplete: Boolean
        get() = _onboardingComplete

    private var _lastSeenVersionCode by mutableIntStateOf(
        preferences.getInt(KEY_LAST_SEEN_VERSION_CODE, 0),
    )

    private var _lastSeenVersionName by mutableStateOf(
        preferences.getString(KEY_LAST_SEEN_VERSION_NAME, null),
    )

    val previousVersionName: String?
        get() = _lastSeenVersionName

    val buildSupportsImages: Boolean
        get() = BuildConfig.SUPPORTS_IMAGES

    /** Compatibility getter used only by the superseded UI file. */
    val imageMode: ImageMode
        get() = if (BuildConfig.SUPPORTS_IMAGES) ImageMode.SHOW else ImageMode.HIDE

    fun setThemeMode(mode: ThemeMode) {
        _themeMode = mode
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    /** Image selection is intentionally disabled; the developer chooses it at build time. */
    @Deprecated("Image support is selected during the GitHub Actions build.")
    fun setImageMode(@Suppress("UNUSED_PARAMETER") mode: ImageMode) = Unit

    fun completeOnboarding(currentVersionCode: Int, currentVersionName: String) {
        _onboardingComplete = true
        preferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .putInt(KEY_LAST_SEEN_VERSION_CODE, currentVersionCode)
            .putString(KEY_LAST_SEEN_VERSION_NAME, currentVersionName)
            .apply()
        _lastSeenVersionCode = currentVersionCode
        _lastSeenVersionName = currentVersionName
    }

    fun shouldShowVersionInformation(currentVersionCode: Int): Boolean {
        return onboardingComplete &&
            !isFreshInstall &&
            _lastSeenVersionCode < currentVersionCode
    }

    fun acknowledgeVersion(currentVersionCode: Int, currentVersionName: String) {
        preferences.edit()
            .putInt(KEY_LAST_SEEN_VERSION_CODE, currentVersionCode)
            .putString(KEY_LAST_SEEN_VERSION_NAME, currentVersionName)
            .apply()
        _lastSeenVersionCode = currentVersionCode
        _lastSeenVersionName = currentVersionName
    }

    companion object {
        private const val PREFERENCES_NAME = "ui_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val KEY_LAST_SEEN_VERSION_NAME = "last_seen_version_name"
    }
}
