package com.aquatrack.app.util

object ValidationUtils {
    fun parseRequiredText(value: CharSequence?): String? {
        val parsed = value?.toString()?.trim().orEmpty()
        return parsed.ifBlank { null }
    }

    fun parsePositiveInt(value: CharSequence?): Int? {
        return value?.toString()?.trim()?.toIntOrNull()?.takeIf { it > 0 }
    }

    fun parsePositiveFloat(value: CharSequence?): Float? {
        return value?.toString()?.trim()?.toFloatOrNull()?.takeIf { it > 0f }
    }

    fun isTemperatureRangeValid(minTemp: Float?, maxTemp: Float?): Boolean {
        return minTemp != null && maxTemp != null && minTemp <= maxTemp
    }
}

