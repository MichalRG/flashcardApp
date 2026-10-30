package com.krzyzowski.vocabboost.reminder

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReminderDecisionTest {
    private val today = LocalDate.parse("2026-11-02")
    private val list = words("puddle", "boon", "thorough", "bargain", "reluctant", "grant")
    private val decision = ReminderDecision(list)

    @Test
    fun `a reminder carries the number of reviews due plus the new words still to introduce`() {
        val deck = DeckProgress.NONE
            .reaching("puddle", Stage.S3, LocalDate.parse("2026-10-20"))
            .reaching("boon", Stage.S7, LocalDate.parse("2026-10-26"))
            .recordAnswer("thorough", knew = true, day = today)

        val workload = decision.decide(deck, today, newWordsPerDay = 3, lastGoalDay = null)

        assertNotNull(workload)
        assertEquals(2, workload!!.reviews.size)
        assertEquals(2, workload.newWords.size)
        assertEquals(4, workload.waiting)
    }

    @Test
    fun `a reminder is still sent when only new words are waiting`() {
        val workload = decision.decide(DeckProgress.NONE, today, newWordsPerDay = 5, lastGoalDay = null)

        assertEquals(5, workload!!.waiting)
        assertEquals(5, workload.newWords.size)
    }

    @Test
    fun `a reminder is still sent when only reviews are due and today's new words are introduced`() {
        val deck = DeckProgress.NONE
            .reaching("puddle", Stage.S3, LocalDate.parse("2026-10-20"))
            .recordAnswer("boon", knew = true, day = today)
            .recordAnswer("thorough", knew = true, day = today)

        val workload = decision.decide(deck, today, newWordsPerDay = 2, lastGoalDay = null)

        assertEquals(1, workload!!.waiting)
        assertEquals(0, workload.newWords.size)
    }

    @Test
    fun `a reminder is skipped after a finished test with no reviews due and today's new words introduced`() {
        val deck = DeckProgress.NONE
            .recordAnswer("puddle", knew = true, day = today)
            .recordAnswer("boon", knew = false, day = today)
            .recordAnswer("thorough", knew = true, day = today)

        assertNull(decision.decide(deck, today, newWordsPerDay = 3, lastGoalDay = null))
    }

    @Test
    fun `a reminder is skipped when no reviews are due and new words are switched off`() {
        val deck = DeckProgress.NONE.reaching("puddle", Stage.S3, today)

        assertNull(decision.decide(deck, today, newWordsPerDay = 0, lastGoalDay = null))
    }

    @Test
    fun `a reminder is skipped when every word is started and none is due`() {
        val deck = list.fold(DeckProgress.NONE) { deck, word -> deck.reaching(word.id, Stage.S14, today) }

        assertNull(decision.decide(deck, today, newWordsPerDay = 5, lastGoalDay = null))
    }

    @Test
    fun `meeting today's goal silences reminders even with overdue reviews and new words waiting`() {
        val deck = DeckProgress.NONE.reaching("puddle", Stage.S3, today.minusDays(10))

        assertNull(decision.decide(deck, today, newWordsPerDay = 5, lastGoalDay = today))
    }

    @Test
    fun `meeting today's goal silences a reminder even when only new words are waiting`() {
        assertNull(decision.decide(DeckProgress.NONE, today, newWordsPerDay = 5, lastGoalDay = today))
    }

    @Test
    fun `yesterday's completed goal does not silence today's reminder`() {
        val workload = decision.decide(
            DeckProgress.NONE, today, newWordsPerDay = 5, lastGoalDay = today.minusDays(1),
        )

        assertEquals(5, workload!!.waiting)
    }

    @Test
    fun `silencing reminders does not remove any due words from voluntary tests`() {
        val deck = DeckProgress.NONE.reaching("puddle", Stage.S3, today.minusDays(10))

        decision.decide(deck, today, newWordsPerDay = 5, lastGoalDay = today)
        val workload = Workload.of(list, deck, today, newWordsPerDay = 5)

        assertEquals(listOf("puddle"), workload.reviews.map { it.id })
        assertEquals(5, workload.newWords.size)
    }
}
