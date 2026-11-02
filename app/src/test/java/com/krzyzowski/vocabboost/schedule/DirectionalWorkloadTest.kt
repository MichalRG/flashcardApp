package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DirectionalWorkloadTest {
    private val today = LocalDate.parse("2026-11-02")
    private val list = words(*(1..10).map { "word-$it" }.toTypedArray())
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST

    @Test
    fun `mixed introduces eight distinct words as sixteen independently learnable cards`() {
        val workload = DirectionalWorkload.of(list, DirectionalProgress.NONE, today, 8, TestMode.MIXED)

        assertEquals(8, workload.newWords.size)
        assertEquals(16, workload.waiting)
        assertEquals(16, workload.cards.map { it.key }.distinct().size)
        assertTrue(workload.cards.all { it.isNew && it.isLearning })
        assertEquals(List(8) { pl } + List(8) { en }, workload.cards.map { it.direction })
    }

    @Test
    fun `fixed modes offer only the selected direction`() {
        listOf(TestMode.POLISH_FIRST to pl, TestMode.ENGLISH_FIRST to en).forEach { (mode, direction) ->
            val workload = DirectionalWorkload.of(list, DirectionalProgress.NONE, today, 8, mode)
            assertEquals(8, workload.waiting)
            assertEquals(List(8) { direction }, workload.cards.map { it.direction })
        }
    }

    @Test
    fun `all due direction cards come before new vocabulary and are not capped`() {
        val reviewIds = (1..30).map { "review-$it" }
        val legacy = reviewIds.fold(DeckProgress.NONE) { deck, id -> deck.reaching(id, Stage.S1, today.minusDays(1)) }
        val deck = DirectionalProgress.fromLegacy(legacy)
        val workload = DirectionalWorkload.of(
            words(*(reviewIds + list.map { it.id }).toTypedArray()), deck, today, 20, TestMode.MIXED,
        )

        assertEquals(60, workload.reviews.size)
        assertEquals(8, workload.newWords.size)
        assertEquals(76, workload.waiting)
        assertTrue(workload.cards.take(60).all { !it.isNew && !it.isLearning })
    }

    @Test
    fun `answering one direction leaves the other direction due`() {
        val legacy = DeckProgress.NONE.reaching("word-1", Stage.S1, today.minusDays(1))
        val deck = DirectionalProgress.fromLegacy(legacy).recordAnswer("word-1", en, true, today)

        assertEquals(0, DirectionalWorkload.of(list, deck, today, 0, TestMode.ENGLISH_FIRST).waiting)
        assertEquals(pl, DirectionalWorkload.of(list, deck, today, 0, TestMode.MIXED).cards.single().direction)
    }

    @Test
    fun `a failed initial direction remains available now even when no new words are allowed`() {
        val deck = DirectionalProgress.NONE.recordAnswer("word-1", pl, false, today)
        val workload = DirectionalWorkload.of(list, deck, today, 0, TestMode.MIXED)

        assertEquals(2, workload.reviews.size)
        assertTrue(workload.cards.all { it.isLearning && !it.isNew })
        assertEquals(0, workload.newWords.size)
    }

    @Test
    fun `learning the other direction does not consume another new-word slot`() {
        var deck = DirectionalProgress.NONE
        list.take(8).forEach { word ->
            deck = deck.recordAnswer(word.id, pl, true, today)
            deck = deck.recordAnswer(word.id, en, true, today)
        }

        assertEquals(8, deck.introducedOn(today))
        assertEquals(0, DirectionalWorkload.of(list, deck, today, 8, TestMode.MIXED).waiting)
        assertEquals(2, DirectionalWorkload.of(list, deck, today.plusDays(1), 8, TestMode.MIXED).newWords.size)
    }

    @Test
    fun `partially introducing a word persists both pending directions and spends only one slot`() {
        val deck = DirectionalProgress.NONE.recordAnswer("word-1", en, false, today)
        val workload = DirectionalWorkload.of(list, deck, today, 8, TestMode.MIXED)

        assertEquals(1, deck.introducedOn(today))
        assertEquals(7, workload.newWords.size)
        assertEquals(2, workload.reviews.size)
        assertFalse(workload.newWords.any { it.id == "word-1" })
    }

    @Test
    fun `empty and retired decks do not produce review cards`() {
        TestMode.entries.forEach { mode ->
            assertEquals(0, DirectionalWorkload.of(emptyList(), DirectionalProgress.NONE, today, 8, mode).waiting)
        }
        val legacy = list.fold(DeckProgress.NONE) { deck, word -> deck.reaching(word.id, Stage.RETIRED, today) }
        assertEquals(0, DirectionalWorkload.of(list, DirectionalProgress.fromLegacy(legacy), today, 8, TestMode.MIXED).waiting)
    }

    @Test
    fun `progress summaries separate pending learning from scheduled stages and tomorrow's reviews`() {
        val deck = DirectionalProgress.NONE.recordAnswer("word-1", en, true, today)
        val english = ProgressSummary.of(list, deck, today, 8, en)
        val polish = ProgressSummary.of(list, deck, today, 8, pl)

        assertEquals(1, english.perStage[Stage.S1])
        assertEquals(1, english.dueTomorrow)
        assertEquals(0, english.learning)
        assertEquals(1, polish.learning)
        assertEquals(1, polish.dueToday)
        assertEquals(0, polish.dueTomorrow)
        assertEquals(0, polish.perStage[Stage.S1])
        assertEquals(7, polish.newToday)
    }
}
