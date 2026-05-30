package com.aquatrack.app.util

import com.aquatrack.app.data.Tank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

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

    @Test
    fun intervalDaysForFrequency_parsesDailyWeeklyAndCustom() {
        assertEquals(1L, ReminderFrequencyUtils.intervalDaysForFrequency("Daily"))
        assertEquals(7L, ReminderFrequencyUtils.intervalDaysForFrequency("Weekly"))
        assertEquals(14L, ReminderFrequencyUtils.intervalDaysForFrequency("Custom:2:Weeks"))
    }

    @Test
    fun isCleaningDue_falseJustBeforeDueTime_evenAfterOneDay() {
        val now = LocalDateTime.of(2026, 5, 29, 18, 59)
        val lastCleaned = LocalDateTime.of(2026, 5, 28, 10, 0)
        val tank = tank(
            lastCleanedEpochMillis = epochMillis(lastCleaned),
            reminderEnabled = true
        )

        assertFalse(ReminderFrequencyUtils.isCleaningDue(tank, now))
    }

    @Test
    fun isCleaningDue_trueAtDueTime() {
        val now = LocalDateTime.of(2026, 5, 29, 19, 0)
        val lastCleaned = LocalDateTime.of(2026, 5, 28, 10, 0)
        val tank = tank(
            lastCleanedEpochMillis = epochMillis(lastCleaned),
            reminderEnabled = true
        )

        assertTrue(ReminderFrequencyUtils.isCleaningDue(tank, now))
    }

    @Test
    fun isCleaningDue_falseWhenReminderDisabled() {
        val now = LocalDateTime.of(2026, 5, 29, 21, 0)
        val lastCleaned = LocalDateTime.of(2026, 5, 28, 10, 0)
        val tank = tank(
            lastCleanedEpochMillis = epochMillis(lastCleaned),
            reminderEnabled = false
        )

        assertFalse(ReminderFrequencyUtils.isCleaningDue(tank, now))
    }

    @Test
    fun nextDueDateTime_rollsForwardWhenNowIsPastFirstDueTime() {
        val now = LocalDateTime.of(2026, 5, 31, 20, 0)
        val lastCleaned = LocalDateTime.of(2026, 5, 29, 9, 0)

        val nextDue = ReminderFrequencyUtils.nextDueDateTime(
            lastCleanedEpochMillis = epochMillis(lastCleaned),
            frequency = "Daily",
            reminderTime = "19:00",
            now = now
        )

        assertEquals(LocalDateTime.of(2026, 6, 1, 19, 0), nextDue)
    }

    private fun tank(
        lastCleanedEpochMillis: Long,
        reminderEnabled: Boolean
    ): Tank {
        return Tank(
            id = 1L,
            name = "Test Tank",
            volumeLitres = 120,
            waterType = "Fresh",
            targetTempC = 25.0f,
            lastCleanedEpochMillis = lastCleanedEpochMillis,
            reminderEnabled = reminderEnabled,
            reminderFrequency = "Daily",
            reminderTime = "19:00",
            imageUri = ""
        )
    }

    private fun epochMillis(value: LocalDateTime): Long {
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
