package com.innocent254.wuwa.companion

import android.app.Application
import com.innocent254.wuwa.companion.core.update.UpdateScheduler

class WuWaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateScheduler.schedule(this)
    }
}
