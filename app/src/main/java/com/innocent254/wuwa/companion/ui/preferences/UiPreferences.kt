package com.innocent254.wuwa.companion.ui.preferences

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ImageMode {
    AUTO,
    SHOW,
    HIDE,
}

class UiPreferences(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private var _themeMode by mutableStateOf(
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
    )

    val themeMode: ThemeMode
        get() = _themeMode

    private var _imageMode by mutableStateOf(
        preferences.getString(KEY_IMAGE_MODE, null)
            ?.let { stored -> ImageMode.entries.firstOrNull { it.name == stored } }
            ?: ImageMode.AUTO,
    )

    val imageMode: ImageMode
        get() = _imageMode

    fun setThemeMode(mode: ThemeMode) {
        _themeMode = mode
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setImageMode(mode: ImageMode) {
        _imageMode = mode
        preferences.edit().putString(KEY_IMAGE_MODE, mode.name).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "ui_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_IMAGE_MODE = "image_mode"
    }
}
