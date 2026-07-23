package com.innocent254.wuwa.companion.ui.preferences

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class DataMode {
    MINIMALIST,
    IMAGES,
}

/** Compatibility type retained for the superseded UI file. */
enum class ImageMode {
    AUTO,
    SHOW,
    HIDE,
}

class UiPreferences(context: Context) {
    private val applicationContext = context.applicationContext
    private val preferences =
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    private val packageInfo = applicationContext.packageManager.getPackageInfo(
        applicationContext.packageName,
        0,
    )
    private val isFreshInstall = packageInfo.firstInstallTime == packageInfo.lastUpdateTime

    private var _themeMode by mutableStateOf(
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
    )

    val themeMode: ThemeMode
        get() = _themeMode

    private var _dataMode by mutableStateOf(
        preferences.getString(KEY_DATA_MODE, null)
            ?.let { stored -> DataMode.entries.firstOrNull { it.name == stored } }
            ?: DataMode.MINIMALIST,
    )

    val dataMode: DataMode
        get() = _dataMode

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

    /** The universal APK always understands optional image packages. */
    val buildSupportsImages: Boolean
        get() = true

    /** Compatibility getter used only by the superseded UI file. */
    val imageMode: ImageMode
        get() = if (dataMode == DataMode.IMAGES) ImageMode.SHOW else ImageMode.HIDE

    fun setThemeMode(mode: ThemeMode) {
        _themeMode = mode
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
        syncSystemSplashTheme(applicationContext, mode)
    }

    fun setDataMode(mode: DataMode) {
        _dataMode = mode
        preferences.edit().putString(KEY_DATA_MODE, mode.name).apply()
    }

    /** Compatibility bridge used only by the superseded UI file. */
    fun setImageMode(mode: ImageMode) {
        setDataMode(
            when (mode) {
                ImageMode.SHOW -> DataMode.IMAGES
                ImageMode.AUTO,
                ImageMode.HIDE -> DataMode.MINIMALIST
            },
        )
    }

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
        const val PREFERENCES_NAME = "ui_preferences"
        const val KEY_DATA_MODE = "data_mode"

        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val KEY_LAST_SEEN_VERSION_NAME = "last_seen_version_name"

        /**
         * Persists the app's theme with Android 12+ so the system starting window
         * uses the same light/dark resource set as the Compose launch animation.
         */
        fun syncSystemSplashTheme(context: Context, mode: ThemeMode = savedThemeMode(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            val systemMode = uiModeManager.nightMode
            val requestedMode = when (mode) {
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                ThemeMode.SYSTEM -> systemMode
            }
            val supportedModes = setOf(
                UiModeManager.MODE_NIGHT_AUTO,
                UiModeManager.MODE_NIGHT_NO,
                UiModeManager.MODE_NIGHT_YES,
                UiModeManager.MODE_NIGHT_CUSTOM,
            )

            uiModeManager.setApplicationNightMode(
                requestedMode.takeIf(supportedModes::contains)
                    ?: UiModeManager.MODE_NIGHT_AUTO,
            )
        }

        private fun savedThemeMode(context: Context): ThemeMode {
            val storedMode = context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            ).getString(KEY_THEME_MODE, null)

            return storedMode
                ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.SYSTEM
        }
    }
}
