package com.aquatrack.app.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aquatrack.app.data.Tank
import com.aquatrack.app.util.ReminderFrequencyUtils
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

object ReminderScheduler {
    fun scheduleOrCancel(context: Context, tank: Tank) {
        val workManager = WorkManager.getInstance(context)
        val uniqueWorkName = uniqueNameForTank(tank.id)

        if (!tank.reminderEnabled || tank.id <= 0L) {
            workManager.cancelUniqueWork(uniqueWorkName)
            return
        }

        val intervalDays = ReminderFrequencyUtils.intervalDaysForFrequency(tank.reminderFrequency)
        val intervalMinutes = max(intervalDays * 24L * 60L, 15L)
        val now = LocalDateTime.now()
        val nextDue = ReminderFrequencyUtils.nextDueDateTime(tank, now)
        val initialDelayMinutes = max(Duration.between(now, nextDue).toMinutes(), 1L)

        val input = Data.Builder()
            .putLong(CleaningReminderWorker.KEY_TANK_ID, tank.id)
            .putString(CleaningReminderWorker.KEY_TANK_NAME, tank.name)
            .build()

        val request = PeriodicWorkRequestBuilder<CleaningReminderWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setInputData(input)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context, tankId: Long) {
        if (tankId <= 0L) return
        WorkManager.getInstance(context).cancelUniqueWork(uniqueNameForTank(tankId))
    }

    private fun uniqueNameForTank(tankId: Long): String = "tank_cleaning_reminder_$tankId"
}
