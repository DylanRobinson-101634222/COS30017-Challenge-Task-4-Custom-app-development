package com.aquatrack.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.aquatrack.app.data.db.AquaTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the "Mark as Cleaned" action tapped directly from a cleaning-reminder notification.
 * Updates the tank's last-cleaned timestamp in the Room database and dismisses the notification
 * — no need to open the app.
 *
 * Uses [goAsync] so the coroutine can complete the database write even if the
 * BroadcastReceiver's normal 10-second window would otherwise expire.
 */
class MarkCleanedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tankId = intent.getLongExtra(EXTRA_TANK_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (tankId < 0L) return

        // goAsync() keeps the receiver alive past onReceive() so the
        // coroutine can safely finish the Room write before calling finish().
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AquaTrackDatabase.getInstance(context)
                val tank = db.tankDao().getById(tankId)
                if (tank != null) {
                    db.tankDao().update(
                        tank.copy(lastCleanedEpochMillis = System.currentTimeMillis())
                    )
                }
            } finally {
                // Always dismiss the notification and release the receiver,
                // even if the DB write somehow fails.
                if (notifId >= 0) {
                    NotificationManagerCompat.from(context).cancel(notifId)
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_CLEANED = "com.aquatrack.app.ACTION_MARK_CLEANED"
        const val EXTRA_TANK_ID = "extra_tank_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}


