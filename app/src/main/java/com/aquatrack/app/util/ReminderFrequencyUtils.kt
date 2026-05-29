package com.aquatrack.app.util

import com.aquatrack.app.data.Tank
import java.util.concurrent.TimeUnit
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

    private fun normaliseUnit(unit: String): String {
        return if (unit.equals(UNIT_WEEKS, ignoreCase = true)) UNIT_WEEKS else UNIT_DAYS
    }

    /**
     * Returns the interval in whole days for the given [frequency] string.
     * Mirrors the logic in ReminderScheduler so it can be reused for UI "overdue" checks.
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
     * Returns true when a tank's reminder is enabled and more days have passed since
     * [Tank.lastCleanedEpochMillis] than the tank's cleaning interval.
     */
    fun isCleaningDue(tank: Tank): Boolean {
        if (!tank.reminderEnabled) return false
        val daysSinceCleaned = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - tank.lastCleanedEpochMillis
        ).coerceAtLeast(0)
        return daysSinceCleaned >= intervalDaysForFrequency(tank.reminderFrequency)
    }
}


