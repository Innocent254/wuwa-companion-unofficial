package com.innocent254.wuwa.companion

import android.app.Application
import com.innocent254.wuwa.companion.core.update.UpdateScheduler
import com.innocent254.wuwa.companion.ui.preferences.UiPreferences

class WuWaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UiPreferences.syncSystemSplashTheme(this)
        UpdateScheduler.schedule(this)
    }
}
