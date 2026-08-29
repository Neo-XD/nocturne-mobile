package com.music.vivi.vivimusic.updater.downloadmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.music.vivi.R
import com.music.vivi.constants.EnableNotificationsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.vivimusic.updater.getDownloadNotificationsSetting
import java.io.File

object DownloadNotificationManager {
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    const val CHANNEL_ID = "download_progress_channel"
    private const val NOTIFICATION_ID = 5678

    fun initialize(context: Context) {
        appContext = context
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.download_progress_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.download_progress_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDownloadStarting(version: String, fileSize: String) {
        val notificationsEnabled = appContext.dataStore.get(EnableNotificationsKey, true)
        if (!notificationsEnabled || !getDownloadNotificationsSetting(appContext)) return
        showDownloadStartingLegacy(version, fileSize)
    }

    fun updateDownloadProgress(progress: Int, version: String) {
        val notificationsEnabled = appContext.dataStore.get(EnableNotificationsKey, true)
        if (!notificationsEnabled || !getDownloadNotificationsSetting(appContext)) return
        updateDownloadProgressLegacy(progress, version)
    }

    fun showDownloadComplete(version: String, filePath: String) {
        val notificationsEnabled = appContext.dataStore.get(EnableNotificationsKey, true)
        if (!notificationsEnabled || !getDownloadNotificationsSetting(appContext)) return
        showDownloadCompleteLegacy(version, filePath)
    }

    fun showDownloadFailed(version: String, errorMessage: String) {
        val notificationsEnabled = appContext.dataStore.get(EnableNotificationsKey, true)
        if (!notificationsEnabled || !getDownloadNotificationsSetting(appContext)) return
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(appContext.getString(R.string.update_failed))
            .setContentText(appContext.getString(R.string.failed_to_download_version, version))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(appContext.getString(R.string.failed_to_download_version_error, version, errorMessage))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        if (::notificationManager.isInitialized) {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun showDownloadStartingLegacy(version: String, fileSize: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.version_file_size, version, fileSize))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateDownloadProgressLegacy(progress: Int, version: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(appContext.getString(R.string.downloading_update))
            .setContentText(appContext.getString(R.string.downloading_percent, progress))
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showDownloadCompleteLegacy(version: String, filePath: String) {
        val file = File(filePath)
        val fileUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.provider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(appContext.getString(R.string.update_ready))
            .setContentText(appContext.getString(R.string.tap_to_install_version, version))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
