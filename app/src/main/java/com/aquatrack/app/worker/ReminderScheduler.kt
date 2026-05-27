package com.aquatrack.app.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aquatrack.app.data.Tank
import com.aquatrack.app.util.ReminderFrequencyUtils
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

        val intervalDays = intervalDaysForFrequency(tank.reminderFrequency)
        val intervalMinutes = max(intervalDays * 24L * 60L, 15L)
        val initialDelayMinutes = max(minutesUntilNextReminder(tank.reminderTime), 1L)

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

    private fun intervalDaysForFrequency(frequency: String): Long {
        ReminderFrequencyUtils.parseCustomFrequency(frequency)?.let { (value, unit) ->
            val days = if (unit.equals("Weeks", ignoreCase = true)) value * 7L else value.toLong()
            return max(days, 1L)
        }

        return when (frequency) {
            "Daily" -> 1L
            else -> 7L
        }
    }


    private fun minutesUntilNextReminder(reminderTime: String): Long {
        val now = LocalDateTime.now()
        val targetTime = runCatching { LocalTime.parse(reminderTime) }.getOrElse { LocalTime.of(19, 0) }
        val todayTarget = LocalDate.now().atTime(targetTime)
        val next = if (todayTarget.isAfter(now)) todayTarget else todayTarget.plusDays(1)
        return Duration.between(now, next).toMinutes()
    }
}
