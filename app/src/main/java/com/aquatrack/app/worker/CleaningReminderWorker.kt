package com.aquatrack.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aquatrack.app.MainActivity
import com.aquatrack.app.R

class CleaningReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val tankName = inputData.getString(KEY_TANK_NAME).orEmpty()
        if (tankName.isBlank()) return Result.success()

        ensureNotificationChannel()

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            tankName.hashCode(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.reminder_notification_title, tankName))
            .setContentText(applicationContext.getString(R.string.reminder_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(tankName.hashCode(), notification)

        return Result.success()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_TANK_ID = "key_tank_id"
        const val KEY_TANK_NAME = "key_tank_name"
        const val CHANNEL_ID = "aquatrack_cleaning_reminders"
    }
}
