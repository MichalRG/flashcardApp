package com.krzyzowski.vocabboost.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WordProgressTest {
    private val introducedOn = LocalDate.parse("2026-10-01")
    private val answerDay = LocalDate.parse("2026-11-02")

    private fun waiting(stage: Stage, due: LocalDate = answerDay) = WordProgress(stage, due, introducedOn)

    @Test
    fun `knowing a word moves it one step and dates it from the answer day plus the new step`() {
        val ladder = listOf(
            Triple(Stage.S1, Stage.S3, 3L),
            Triple(Stage.S3, Stage.S7, 7L),
            Triple(Stage.S7, Stage.S14, 14L),
            Triple(Stage.S14, Stage.S30, 30L),
            Triple(Stage.S30, Stage.C60, 60L),
            Triple(Stage.C60, Stage.C90, 90L),
            Triple(Stage.C90, Stage.C180, 180L),
            Triple(Stage.C180, Stage.C365, 365L),
            Triple(Stage.C365, Stage.MAINTENANCE, 365L),
            Triple(Stage.MAINTENANCE, Stage.MAINTENANCE, 365L),
        )

        ladder.forEach { (from, to, days) ->
            val after = waiting(from).answered(knew = true, day = answerDay)

            assertEquals("stage after $from", to, after.stage)
            assertEquals("due after $from", answerDay.plusDays(days), after.due)
        }
    }

    @Test
    fun `maintenance remains due indefinitely until answered and then schedules another 365 days`() {
        var progress = waiting(Stage.MAINTENANCE)
        var day = LocalDate.parse("2126-01-01")
        repeat(10) {
            assertTrue(progress.isDueBy(day))
            progress = progress.answered(knew = true, day)
            assertEquals(Stage.MAINTENANCE, progress.stage)
            assertEquals(day.plusDays(365), progress.due)
            assertEquals(introducedOn, progress.introducedOn)
            assertFalse(progress.isDueBy(day.plusDays(364)))
            day = day.plusDays(365)
        }
    }

    @Test
    fun `a legacy retired record without a due date becomes immediately due maintenance`() {
        val legacy = WordProgress(Stage.fromCode("RET"), null, introducedOn)

        assertEquals(Stage.MAINTENANCE, legacy.stage)
        assertTrue(legacy.isDueBy(answerDay))
        assertTrue(legacy.isDueBy(answerDay.plusYears(10)))
        val remembered = legacy.answered(knew = true, answerDay)
        assertEquals(WordProgress(Stage.MAINTENANCE, answerDay.plusDays(365), introducedOn), remembered)
        assertEquals(WordProgress(Stage.S1, answerDay.plusDays(1), introducedOn), legacy.answered(false, answerDay))
    }

    @Test
    fun `late maintenance schedules 365 calendar days from the actual answer even across a leap day`() {
        val late = waiting(Stage.MAINTENANCE, LocalDate.parse("2028-02-28"))
            .answered(knew = true, LocalDate.parse("2028-02-29"))

        assertEquals(Stage.MAINTENANCE, late.stage)
        assertEquals(LocalDate.parse("2029-02-28"), late.due)
        assertFalse(late.isDueBy(LocalDate.parse("2029-02-27")))
        assertTrue(late.isDueBy(LocalDate.parse("2029-02-28")))
    }

    @Test
    fun `not knowing a word at any step sends it back to the 1-day step due tomorrow`() {
        Stage.entries.forEach { from ->
            val after = waiting(from).answered(knew = false, day = answerDay)

            assertEquals("stage after failing $from", Stage.S1, after.stage)
            assertEquals("due after failing $from", LocalDate.parse("2026-11-03"), after.due)
        }
    }

    @Test
    fun `a failed word climbs again through 3 then 7 days`() {
        val failed = waiting(Stage.S30).answered(knew = false, day = answerDay)
        val passedOnce = failed.answered(knew = true, day = LocalDate.parse("2026-11-03"))
        val passedTwice = passedOnce.answered(knew = true, day = LocalDate.parse("2026-11-06"))

        assertEquals(WordProgress(Stage.S3, LocalDate.parse("2026-11-06"), introducedOn), passedOnce)
        assertEquals(WordProgress(Stage.S7, LocalDate.parse("2026-11-13"), introducedOn), passedTwice)
    }

    @Test
    fun `a word answered late and known is dated from the answer day not from the missed due date`() {
        val missedBy9Days = waiting(Stage.S3, due = LocalDate.parse("2026-11-02"))

        val after = missedBy9Days.answered(knew = true, day = LocalDate.parse("2026-11-11"))

        assertEquals(Stage.S7, after.stage)
        assertEquals(LocalDate.parse("2026-11-18"), after.due)
    }

    @Test
    fun `a word answered late and not known is due the day after the answer`() {
        val missedBy9Days = waiting(Stage.S14, due = LocalDate.parse("2026-11-02"))

        val after = missedBy9Days.answered(knew = false, day = LocalDate.parse("2026-11-11"))

        assertEquals(Stage.S1, after.stage)
        assertEquals(LocalDate.parse("2026-11-12"), after.due)
    }

    @Test
    fun `answering keeps the day the word was first introduced`() {
        val after = waiting(Stage.S7).answered(knew = false, day = answerDay)

        assertEquals(introducedOn, after.introducedOn)
    }

    @Test
    fun `a word is due on its due date and every day after, but not the day before`() {
        val progress = waiting(Stage.S7, due = LocalDate.parse("2026-11-10"))

        assertFalse(progress.isDueBy(LocalDate.parse("2026-11-09")))
        assertTrue(progress.isDueBy(LocalDate.parse("2026-11-10")))
        assertTrue(progress.isDueBy(LocalDate.parse("2026-11-25")))
    }
}
