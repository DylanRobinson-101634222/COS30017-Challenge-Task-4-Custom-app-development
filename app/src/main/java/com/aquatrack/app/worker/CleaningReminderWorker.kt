package com.aquatrack.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aquatrack.app.MainActivity
import com.aquatrack.app.R

class CleaningReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val tankId = inputData.getLong(KEY_TANK_ID, -1L)
        val tankName = inputData.getString(KEY_TANK_NAME).orEmpty()
        if (tankName.isBlank()) return Result.success()

        ensureNotificationChannel()

        // Unique, stable ID for this tank's notification — used both to post
        // and to let the action button dismiss it afterwards.
        val notifId = tankName.hashCode()

        // Tapping the notification body opens the app.
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            notifId,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Mark as Cleaned" action button - handled by MarkCleanedReceiver.
        val markCleanedIntent = Intent(applicationContext, MarkCleanedReceiver::class.java).apply {
            action = MarkCleanedReceiver.ACTION_MARK_CLEANED
            putExtra(MarkCleanedReceiver.EXTRA_TANK_ID, tankId)
            putExtra(MarkCleanedReceiver.EXTRA_NOTIF_ID, notifId)
        }
        val markCleanedPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notifId,
            markCleanedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.reminder_notification_title, tankName))
            .setContentText(applicationContext.getString(R.string.reminder_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                applicationContext.getString(R.string.notification_action_mark_cleaned),
                markCleanedPendingIntent
            )
            .build()

        // On Android 13+ the user must have granted POST_NOTIFICATIONS; skip silently if not.
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (canPost) {
            NotificationManagerCompat.from(applicationContext).notify(notifId, notification)
        }

        return Result.success()
    }

    private fun ensureNotificationChannel() {
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
