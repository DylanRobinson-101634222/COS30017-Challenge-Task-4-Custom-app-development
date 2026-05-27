package com.aquatrack.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderFrequencyUtilsTest {
    @Test
    fun parseCustomFrequency_validValue_parsesCorrectly() {
        val parsed = ReminderFrequencyUtils.parseCustomFrequency("Custom:3:Weeks")

        assertEquals(3 to ReminderFrequencyUtils.UNIT_WEEKS, parsed)
    }

    @Test
    fun parseCustomFrequency_invalidUnit_returnsNull() {
        val parsed = ReminderFrequencyUtils.parseCustomFrequency("Custom:3:Months")

        assertNull(parsed)
    }

    @Test
    fun buildCustomFrequency_normalisesUnitAndClampsValue() {
        val built = ReminderFrequencyUtils.buildCustomFrequency(0, "weeks")

        assertEquals("Custom:1:Weeks", built)
    }

    @Test
    fun resolveSavedFrequency_keepsCustomPayloadWhenSelectedCustom() {
        val resolved = ReminderFrequencyUtils.resolveSavedFrequency(
            initialFrequency = "Custom:5:Days",
            selectedFrequency = "Custom"
        )

        assertEquals("Custom:5:Days", resolved)
    }

    @Test
    fun resolveSavedFrequency_usesSelectedValueWhenNotCustomSelection() {
        val resolved = ReminderFrequencyUtils.resolveSavedFrequency(
            initialFrequency = "Custom:5:Days",
            selectedFrequency = "Weekly"
        )

        assertEquals("Weekly", resolved)
    }
}

