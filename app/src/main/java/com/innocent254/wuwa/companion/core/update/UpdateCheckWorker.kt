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
import com.innocent254.wuwa.companion.R

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences(
            "update_versions",
            Context.MODE_PRIVATE,
        )
        val databaseVersion = preferences.getString("database_version", "0.0.0") ?: "0.0.0"
        val assetVersion = preferences.getString("asset_version", "0.0.0") ?: "0.0.0"

        val availability = runCatching {
            UpdateRepository(applicationContext).check(
                currentAppVersionCode = BuildConfig.VERSION_CODE,
                currentDatabaseVersion = databaseVersion,
                currentAssetVersion = assetVersion,
            )
        }.getOrElse { return Result.retry() }

        if (
            availability.appUpdateAvailable ||
            availability.databaseUpdateAvailable ||
            availability.assetUpdateAvailable
        ) {
            showNotification(availability)
        }
        return Result.success()
    }

    private fun showNotification(availability: UpdateAvailability) {
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
            if (availability.assetUpdateAvailable) add("assets")
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
