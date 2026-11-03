package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProgressSummaryTest {
    private val today = LocalDate.parse("2026-11-02")

    @Test
    fun `progress counts words at each stage and the reviews due today and tomorrow`() {
        val list = words("fat", "cope", "s1-due", "s1-new", "s3-late", "s7", "s14", "s30", "c60", "c90", "done")
        val deck = DeckProgress.NONE
            .reaching("s1-due", Stage.S1, LocalDate.parse("2026-11-01"))
            .reaching("s1-new", Stage.S1, today)
            .reaching("s3-late", Stage.S3, LocalDate.parse("2026-10-20"))
            .reaching("s7", Stage.S7, LocalDate.parse("2026-10-30"))
            .reaching("s14", Stage.S14, LocalDate.parse("2026-10-30"))
            .reaching("s30", Stage.S30, LocalDate.parse("2026-10-30"))
            .reaching("c60", Stage.C60, LocalDate.parse("2026-10-30"))
            .reaching("c90", Stage.C90, LocalDate.parse("2026-10-30"))
            .reaching("done", Stage.MAINTENANCE, LocalDate.parse("2026-10-01"))
            .reaching("removed-from-list", Stage.S3, LocalDate.parse("2026-10-15"))

        val summary = ProgressSummary.of(list, deck, today, newWordsPerDay = 3)

        val expected = ProgressSummary(
            notStarted = 2,
            perStage = mapOf(
                Stage.S1 to 2,
                Stage.S3 to 1,
                Stage.S7 to 1,
                Stage.S14 to 1,
                Stage.S30 to 1,
                Stage.C60 to 1,
                Stage.C90 to 1,
                Stage.C180 to 0,
                Stage.C365 to 0,
                Stage.MAINTENANCE to 1,
            ),
            dueToday = 2, // s1-due is due today, s3-late is overdue; the removed word is not counted
            newToday = 2, // quota 3 minus s1-new, which was answered today
            dueTomorrow = 1, // only s1-new is dated tomorrow
        )
        assertEquals(expected, summary)
    }

    @Test
    fun `on a fresh install every word is not started and today's quota of new words is waiting`() {
        val list = words("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")

        val summary = ProgressSummary.of(list, DeckProgress.NONE, today, newWordsPerDay = 5)

        assertEquals(10, summary.notStarted)
        assertEquals(Stage.entries.associateWith { 0 }, summary.perStage)
        assertEquals(0, summary.dueToday)
        assertEquals(5, summary.newToday)
        assertEquals(0, summary.dueTomorrow)
    }
}
