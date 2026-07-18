package com.innocent254.wuwa.companion.core.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.innocent254.wuwa.companion.BuildConfig
import com.innocent254.wuwa.companion.ui.preferences.DataMode
import com.innocent254.wuwa.companion.ui.preferences.UiPreferences

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val versionPreferences = applicationContext.getSharedPreferences(
            "update_versions",
            Context.MODE_PRIVATE,
        )
        val databaseVersion = versionPreferences.getString("database_version", "0.0.0") ?: "0.0.0"
        val assetVersion = versionPreferences.getString("asset_version", "0.0.0") ?: "0.0.0"
        val uiPreferences = applicationContext.getSharedPreferences(
            UiPreferences.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val dataMode = uiPreferences.getString(UiPreferences.KEY_DATA_MODE, null)
            ?.let { stored -> DataMode.entries.firstOrNull { it.name == stored } }
            ?: DataMode.MINIMALIST

        val availability = runCatching {
            UpdateRepository(applicationContext).check(
                currentAppVersionCode = BuildConfig.VERSION_CODE,
                currentDatabaseVersion = databaseVersion,
                currentAssetVersion = assetVersion,
            )
        }.getOrElse { return Result.retry() }

        val selectedAssetUpdate = dataMode == DataMode.IMAGES && availability.assetUpdateAvailable
        if (
            availability.appUpdateAvailable ||
            availability.databaseUpdateAvailable ||
            selectedAssetUpdate
        ) {
            showNotification(
                availability = availability,
                includeAssets = selectedAssetUpdate,
            )
        }
        return Result.success()
    }

    private fun showNotification(
        availability: UpdateAvailability,
        includeAssets: Boolean,
    ) {
        createChannel()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val parts = buildList {
            if (availability.appUpdateAvailable) add("app")
            if (availability.databaseUpdateAvailable) add("database")
            if (includeAssets) add("images")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("WuWa Companion update available")
            .setContentText("New ${parts.joinToString(" and ")} update detected.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private companion object {
        const val CHANNEL_ID = "updates"
        const val NOTIFICATION_ID = 1001
    }
}
