package com.aquatrack.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {
    @Test
    fun parseRequiredText_trimsAndReturnsValue() {
        val parsed = ValidationUtils.parseRequiredText("  Reef Tank  ")

        assertEquals("Reef Tank", parsed)
    }

    @Test
    fun parseRequiredText_blankReturnsNull() {
        val parsed = ValidationUtils.parseRequiredText("   ")

        assertNull(parsed)
    }

    @Test
    fun parsePositiveInt_positiveReturnsValue() {
        val parsed = ValidationUtils.parsePositiveInt("12")

        assertEquals(12, parsed)
    }

    @Test
    fun parsePositiveInt_zeroReturnsNull() {
        val parsed = ValidationUtils.parsePositiveInt("0")

        assertNull(parsed)
    }

    @Test
    fun isTemperatureRangeValid_validRange_returnsTrue() {
        assertTrue(ValidationUtils.isTemperatureRangeValid(24.0f, 26.5f))
    }

    @Test
    fun isTemperatureRangeValid_minAboveMax_returnsFalse() {
        assertFalse(ValidationUtils.isTemperatureRangeValid(27.0f, 26.5f))
    }
}

