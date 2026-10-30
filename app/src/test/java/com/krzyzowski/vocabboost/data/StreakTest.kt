package com.krzyzowski.vocabboost.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    private val today = LocalDate.parse("2026-11-02")

    @Test
    fun `the first goal day starts the streak at one`() {
        assertEquals(Streak(1, today), Streak.NONE.goalMetOn(today))
    }

    @Test
    fun `goal days on consecutive days build the streak`() {
        val streak = Streak.NONE
            .goalMetOn(today.minusDays(2))
            .goalMetOn(today.minusDays(1))
            .goalMetOn(today)

        assertEquals(Streak(3, today), streak)
    }

    @Test
    fun `meeting the goal again on the same day does not count the day twice`() {
        val streak = Streak.NONE.goalMetOn(today.minusDays(1)).goalMetOn(today).goalMetOn(today)

        assertEquals(Streak(2, today), streak)
    }

    @Test
    fun `a goal day after a missed day starts the streak again at one`() {
        val streak = Streak(count = 6, lastGoalDay = today.minusDays(2)).goalMetOn(today)

        assertEquals(Streak(1, today), streak)
    }

    @Test
    fun `a day earlier than the last goal day does not change the streak`() {
        val streak = Streak(count = 3, lastGoalDay = today)

        assertEquals(streak, streak.goalMetOn(today.minusDays(1)))
    }

    @Test
    fun `the streak keeps yesterday's number until today's goal is met`() {
        val streak = Streak(count = 4, lastGoalDay = today.minusDays(1))

        assertEquals(4, streak.shownOn(today))
        assertEquals(5, streak.goalMetOn(today).shownOn(today))
    }

    @Test
    fun `the streak drops to zero after a missed day`() {
        val streak = Streak(count = 4, lastGoalDay = today.minusDays(2))

        assertEquals(0, streak.shownOn(today))
        assertEquals(0, Streak.NONE.shownOn(today))
    }

    @Test
    fun `a streak is saved as count and last goal day and reads back`() {
        assertEquals("4;2026-11-02", Streak(4, today).encode())
        assertEquals(Streak(4, today), Streak.decode("4;2026-11-02"))
        assertEquals("0;-", Streak.NONE.encode())
        assertEquals(Streak.NONE, Streak.decode("0;-"))
    }

    @Test
    fun `an unreadable saved streak reads as no streak`() {
        assertEquals(Streak.NONE, Streak.decode("many;2026-11-02"))
        assertEquals(Streak.NONE, Streak.decode(""))
    }
}
