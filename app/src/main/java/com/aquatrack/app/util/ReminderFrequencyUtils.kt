package com.aquatrack.app.util

import com.aquatrack.app.data.Tank
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

object ReminderFrequencyUtils {
    private const val CUSTOM_PREFIX = "Custom:"
    private const val CUSTOM_LABEL = "Custom"
    const val UNIT_DAYS = "Days"
    const val UNIT_WEEKS = "Weeks"
    const val DEFAULT_CUSTOM_VALUE = 2
    private val customFrequencyRegex = Regex("^Custom:(\\d+):(Days|Weeks)$", RegexOption.IGNORE_CASE)

    fun parseCustomFrequency(frequency: String): Pair<Int, String>? {
        val match = customFrequencyRegex.matchEntire(frequency.trim()) ?: return null
        val value = match.groupValues[1].toIntOrNull() ?: return null
        if (value <= 0) return null
        val unit = normaliseUnit(match.groupValues[2])
        return value to unit
    }

    fun buildCustomFrequency(value: Int, unit: String): String {
        val safeValue = value.coerceAtLeast(1)
        return "$CUSTOM_PREFIX$safeValue:${normaliseUnit(unit)}"
    }

    fun resolveSavedFrequency(initialFrequency: String, selectedFrequency: String): String {
        if (selectedFrequency.equals(CUSTOM_LABEL, ignoreCase = true) &&
            parseCustomFrequency(initialFrequency) != null
        ) {
            return initialFrequency
        }
        return selectedFrequency
    }

    /**
     * Returns the interval in whole days for the given [frequency] string.
     */
    fun intervalDaysForFrequency(frequency: String): Long {
        parseCustomFrequency(frequency)?.let { (value, unit) ->
            val days = if (unit.equals(UNIT_WEEKS, ignoreCase = true)) value * 7L else value.toLong()
            return max(days, 1L)
        }
        return when (frequency) {
            "Daily" -> 1L
            else -> 7L
        }
    }

    /**
     * Calculates when the next clean becomes due based on last cleaned date,
     * selected frequency, and the configured reminder time.
     */
    fun nextDueDateTime(tank: Tank, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        return nextDueDateTime(
            lastCleanedEpochMillis = tank.lastCleanedEpochMillis,
            frequency = tank.reminderFrequency,
            reminderTime = tank.reminderTime,
            now = now
        )
    }

    fun nextDueDateTime(
        lastCleanedEpochMillis: Long,
        frequency: String,
        reminderTime: String,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        val intervalDays = intervalDaysForFrequency(frequency)
        val targetTime = parseReminderTime(reminderTime)
        val zone = ZoneId.systemDefault()
        val lastCleanedDate = Instant.ofEpochMilli(lastCleanedEpochMillis)
            .atZone(zone)
            .toLocalDate()

        var nextDue = lastCleanedDate.plusDays(intervalDays).atTime(targetTime)
        while (!nextDue.isAfter(now)) {
            nextDue = nextDue.plusDays(intervalDays)
        }
        return nextDue
    }

    /**
     * Returns true when reminders are enabled and the tank has reached/passed
     * the scheduled due time for cleaning.
     */
    fun isCleaningDue(tank: Tank, now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (!tank.reminderEnabled) return false
        val intervalDays = intervalDaysForFrequency(tank.reminderFrequency)
        val targetTime = parseReminderTime(tank.reminderTime)
        val zone = ZoneId.systemDefault()
        val lastCleanedDate = Instant.ofEpochMilli(tank.lastCleanedEpochMillis)
            .atZone(zone)
            .toLocalDate()
        val dueAt = lastCleanedDate.plusDays(intervalDays).atTime(targetTime)
        return !now.isBefore(dueAt)
    }

    private fun parseReminderTime(reminderTime: String): LocalTime {
        return runCatching { LocalTime.parse(reminderTime) }.getOrElse { LocalTime.of(19, 0) }
    }

    private fun normaliseUnit(unit: String): String {
        return if (unit.equals(UNIT_WEEKS, ignoreCase = true)) UNIT_WEEKS else UNIT_DAYS
    }
}
