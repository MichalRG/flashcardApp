package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceProgressTest {
    private val today = LocalDate.parse("2026-11-03")
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST
    private val legacyDirectional =
        "ENGLISH_FIRST=boon=RET;-;2026-03-01|\nPOLISH_FIRST=boon=C365;2027-11-03;2026-03-01|"

    @Test
    fun `new long-term stages round-trip through both directional schedules`() {
        listOf(Stage.C180, Stage.C365, Stage.MAINTENANCE).forEach { stage ->
            val text = "boon=${stage.code};2027-11-03;2026-03-01"
            val deck = DirectionalProgress.fromLegacy(DeckProgress.decode(text))
            val restored = DirectionalProgress.decode(deck.encode())

            CardDirection.entries.forEach { direction ->
                assertEquals(stage, restored.of("boon", direction)?.stage)
                assertEquals(LocalDate.parse("2027-11-03"), restored.of("boon", direction)?.due)
                assertEquals(text, restored.forDirection(direction).schedule.encode())
                assertFalse(restored.isLearning("boon", direction))
            }
        }
    }

    @Test
    fun `legacy retired vocabulary returns in both directions without spending new-word allowance`() {
        val deck = DirectionalProgress.fromLegacy(DeckProgress.decode("boon=RET;-;2026-03-01"))
        val workload = DirectionalWorkload.of(words("boon", "puddle"), deck, today, 1, TestMode.MIXED)

        assertEquals(2, workload.reviews.size)
        assertEquals(setOf(en, pl), workload.reviews.map { it.direction }.toSet())
        assertTrue(workload.reviews.all { !it.isLearning && !it.isNew })
        assertEquals(listOf("puddle"), workload.newWords.map { it.id })
        assertEquals(0, deck.introducedOn(today))
        assertEquals(4, workload.waiting)
    }

    @Test
    fun `a maintenance success schedules only that direction without changing the other direction`() {
        val before = DirectionalProgress.decode(legacyDirectional)
        val after = before.recordAnswer("boon", en, true, today)

        assertEquals(Stage.MAINTENANCE, after.of("boon", en)?.stage)
        assertEquals(today.plusDays(365), after.of("boon", en)?.due)
        assertEquals(LocalDate.parse("2026-03-01"), after.of("boon", en)?.introducedOn)
        assertEquals(before.of("boon", pl), after.of("boon", pl))
        assertEquals(legacyDirectional, before.encode())
        assertEquals(0, DirectionalWorkload.of(words("boon"), after, today, 0, TestMode.MIXED).waiting)
    }

    @Test
    fun `dated maintenance appears on its date and in progress counts without ever being permanently removed`() {
        val due = today.plusDays(365)
        val deck = DirectionalProgress.fromLegacy(DeckProgress.decode("boon=RET;$due;2026-03-01"))
        val list = words("boon")

        assertEquals(0, DirectionalWorkload.of(list, deck, due.minusDays(1), 0, TestMode.MIXED).waiting)
        assertEquals(2, DirectionalWorkload.of(list, deck, due, 0, TestMode.MIXED).waiting)
        assertEquals(2, DirectionalWorkload.of(list, deck, due.plusYears(100), 0, TestMode.MIXED).waiting)
        assertEquals(1, ProgressSummary.of(list, deck, due.minusDays(1), 0, en).dueTomorrow)
        val summary = ProgressSummary.of(list, deck, due, 0, en)
        assertEquals(1, summary.perStage[Stage.MAINTENANCE])
        assertEquals(1, summary.dueToday)
        assertEquals(0, summary.newToday)
        assertEquals(0, summary.learning)
    }

    @Test
    fun `a missed maintenance review returns tomorrow as a mature review in only its own direction`() {
        val before = DirectionalProgress.decode(legacyDirectional)
        val after = before.recordAnswer("boon", en, false, today)

        assertEquals(Stage.S1, after.of("boon", en)?.stage)
        assertEquals(today.plusDays(1), after.of("boon", en)?.due)
        assertEquals(before.of("boon", pl), after.of("boon", pl))
        assertFalse(after.isLearning("boon", en))
        assertEquals(0, DirectionalWorkload.of(words("boon"), after, today, 0, TestMode.ENGLISH_FIRST).waiting)
        val review = DirectionalWorkload.of(words("boon"), after, today.plusDays(1), 0, TestMode.MIXED).cards.single()
        assertEquals(en, review.direction)
        assertFalse(review.isNew)
        assertFalse(review.isLearning)
    }
}
