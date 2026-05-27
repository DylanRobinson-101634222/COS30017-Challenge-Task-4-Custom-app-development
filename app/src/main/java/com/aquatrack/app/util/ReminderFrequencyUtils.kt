package com.aquatrack.app.util

object ReminderFrequencyUtils {
    private const val CUSTOM_PREFIX = "Custom:"
    private const val CUSTOM_VALUE_SEPARATOR = ":"
    private const val CUSTOM_LABEL = "Custom"

    fun parseCustomFrequency(frequency: String): Pair<Int, String>? {
        if (!frequency.startsWith(CUSTOM_PREFIX, ignoreCase = true)) return null

        val parts = frequency.split(CUSTOM_VALUE_SEPARATOR)
        if (parts.size < 3) return null

        val value = parts[1].toIntOrNull() ?: return null
        val unit = parts[2]
        return value to unit
    }

    fun resolveSavedFrequency(initialFrequency: String, selectedFrequency: String): String {
        if (selectedFrequency.equals(CUSTOM_LABEL, ignoreCase = true) &&
            parseCustomFrequency(initialFrequency) != null
        ) {
            return initialFrequency
        }
        return selectedFrequency
    }
}


