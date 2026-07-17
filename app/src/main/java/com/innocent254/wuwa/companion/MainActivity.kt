package com.innocent254.wuwa.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.innocent254.wuwa.companion.ui.WuWaApp
import com.innocent254.wuwa.companion.ui.model.DemoUiStateFactory
import com.innocent254.wuwa.companion.ui.preferences.UiPreferences
import com.innocent254.wuwa.companion.ui.theme.WuWaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val applicationContext = LocalContext.current.applicationContext
            val preferences = remember(applicationContext) {
                UiPreferences(applicationContext)
            }
            val uiState = remember { DemoUiStateFactory.create() }

            WuWaTheme(themeMode = preferences.themeMode) {
                WuWaApp(
                    uiState = uiState,
                    preferences = preferences,
                )
            }
        }
    }
}
