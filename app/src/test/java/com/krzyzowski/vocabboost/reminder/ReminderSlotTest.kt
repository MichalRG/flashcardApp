package com.krzyzowski.vocabboost.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReminderSlotTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")

    @Test
    fun `there are exactly two reminders, at 08 and 17 hours, each with its own alarm identity`() {
        assertEquals(listOf(ReminderSlot.MORNING, ReminderSlot.EVENING), ReminderSlot.entries)
        assertEquals(listOf(8, 17), ReminderSlot.entries.map { it.hour })
        assertEquals(2, ReminderSlot.entries.map { it.requestCode }.distinct().size)
    }

    @Test
    fun `the next reminder is the next local slot strictly after now`() {
        // Warsaw is UTC+1 in November
        val cases = listOf(
            Triple(ReminderSlot.MORNING, "2026-11-02T06:59:00Z", "2026-11-02T07:00:00Z"),
            Triple(ReminderSlot.MORNING, "2026-11-02T07:00:00Z", "2026-11-03T07:00:00Z"),
            Triple(ReminderSlot.MORNING, "2026-11-02T20:00:00Z", "2026-11-03T07:00:00Z"),
            Triple(ReminderSlot.EVENING, "2026-11-02T06:00:00Z", "2026-11-02T16:00:00Z"),
            Triple(ReminderSlot.EVENING, "2026-11-02T15:59:59Z", "2026-11-02T16:00:00Z"),
            Triple(ReminderSlot.EVENING, "2026-11-02T16:00:00Z", "2026-11-03T16:00:00Z"),
        )

        cases.forEach { (slot, now, expected) ->
            assertEquals("$slot after $now", Instant.parse(expected), slot.nextTrigger(Instant.parse(now), warsaw))
        }
    }

    @Test
    fun `the reminder stays at local 08 and 17 hours on the day the clocks go forward`() {
        // 29 March 2026: Warsaw moves from UTC+1 to UTC+2 at 02:00
        val eveningBefore = Instant.parse("2026-03-28T19:00:00Z")
        val morning = ReminderSlot.MORNING.nextTrigger(eveningBefore, warsaw)
        val evening = ReminderSlot.EVENING.nextTrigger(morning, warsaw)

        assertEquals(Instant.parse("2026-03-29T06:00:00Z"), morning)
        assertEquals(Instant.parse("2026-03-29T15:00:00Z"), evening)
    }

    @Test
    fun `the reminder stays at local 08 and 17 hours on the day the clocks go back`() {
        // 25 October 2026: Warsaw moves from UTC+2 to UTC+1 at 03:00
        val eveningBefore = Instant.parse("2026-10-24T19:00:00Z")
        val morning = ReminderSlot.MORNING.nextTrigger(eveningBefore, warsaw)
        val evening = ReminderSlot.EVENING.nextTrigger(morning, warsaw)

        assertEquals(Instant.parse("2026-10-25T07:00:00Z"), morning)
        assertEquals(Instant.parse("2026-10-25T16:00:00Z"), evening)
    }

    @Test
    fun `a change of time zone moves the next reminder to the new local time`() {
        val now = Instant.parse("2026-06-10T12:00:00Z")

        val inWarsaw = ReminderSlot.EVENING.nextTrigger(now, warsaw)
        val inNewYork = ReminderSlot.EVENING.nextTrigger(now, ZoneId.of("America/New_York"))

        assertEquals(Instant.parse("2026-06-10T15:00:00Z"), inWarsaw)
        assertEquals(Instant.parse("2026-06-10T21:00:00Z"), inNewYork)
    }
}
