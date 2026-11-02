package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class TestDeckBuilderTest {
    private val answeredOn = LocalDate.parse("2026-10-01")
    private val builder = TestDeckBuilder(Random(7))

    @Test
    fun `English-only mode shows every review and new word English first`() {
        val list = words("anchor", "pearl", "kettle", "lantern")
        val workload = Workload(reviews = list.take(2), newWords = list.drop(2))

        val cards = builder.build(workload, TestMode.ENGLISH_FIRST)

        assertEquals(list.map { it.id }, cards.map { it.word.id })
        assertEquals(List(4) { CardDirection.ENGLISH_FIRST }, cards.map { it.direction })
    }

    @Test
    fun `Polish-only mode shows every review and new word Polish first`() {
        val list = words("anchor", "pearl", "kettle", "lantern")
        val workload = Workload(reviews = list.take(2), newWords = list.drop(2))

        val cards = builder.build(workload, TestMode.POLISH_FIRST)

        assertEquals(list.map { it.id }, cards.map { it.word.id })
        assertEquals(List(4) { CardDirection.POLISH_FIRST }, cards.map { it.direction })
    }

    @Test
    fun `mixed mode gives two thirds Polish-first cards without reordering or repeating words`() {
        val list = words(*(1..9).map { "word-$it" }.toTypedArray())
        val workload = Workload(reviews = list.take(6), newWords = list.drop(6))

        val cards = builder.build(workload, TestMode.MIXED)

        assertEquals(list.map { it.id }, cards.map { it.word.id })
        assertEquals(9, cards.map { it.word.id }.toSet().size)
        assertEquals(6, cards.count { it.direction == CardDirection.POLISH_FIRST })
        assertEquals(3, cards.count { it.direction == CardDirection.ENGLISH_FIRST })
        assertEquals(List(6) { false } + List(3) { true }, cards.map { it.isNew })
    }

    @Test
    fun `eight new words mix five Polish-first and three English-first prompts`() {
        val list = words(*(1..8).map { "new-$it" }.toTypedArray())

        val cards = builder.build(Workload(emptyList(), list), TestMode.MIXED)

        assertEquals(8, cards.size)
        assertEquals(5, cards.count { it.direction == CardDirection.POLISH_FIRST })
        assertEquals(3, cards.count { it.direction == CardDirection.ENGLISH_FIRST })
        assertEquals(List(8) { true }, cards.map { it.isNew })
    }

    @Test
    fun `small mixed tests round to the nearest two-thirds ratio and prefer Polish for a single card`() {
        val expectedPolishCounts = listOf(1, 1, 2, 3, 3, 4, 5, 5, 6, 7)
        expectedPolishCounts.forEachIndexed { index, expected ->
            val list = words(*(1..index + 1).map { "word-$it" }.toTypedArray())
            val cards = builder.build(Workload(list, emptyList()), TestMode.MIXED)

            assertEquals("Polish cards in a test of ${list.size}", expected, cards.count {
                it.direction == CardDirection.POLISH_FIRST
            })
            assertEquals(list.size, cards.size)
        }
    }

    @Test
    fun `mixed directions are fixed on the built cards instead of changing when a card is flipped`() {
        val list = words(*(1..9).map { "word-$it" }.toTypedArray())
        val cards = builder.build(Workload(list, emptyList()), TestMode.MIXED)
        val initialDirections = cards.map { it.direction }

        cards.forEach { it.direction.front(it.word) }

        assertEquals(initialDirections, cards.map { it.direction })
    }

    @Test
    fun `answering in one direction advances the shared schedule and switching modes does not offer it again`() {
        val list = words("anchor")
        val today = answeredOn.plusDays(1)
        val deck = DeckProgress.NONE.reaching("anchor", Stage.S1, answeredOn)
        val workload = Workload.of(list, deck, today, newWordsPerDay = 8)
        val card = builder.build(workload, TestMode.POLISH_FIRST).single()

        val answered = deck.recordAnswer(card.word.id, knew = true, day = today)
        val remaining = Workload.of(list, answered, today, newWordsPerDay = 8)

        assertEquals(Stage.S3, answered.of("anchor")?.stage)
        assertEquals(today.plusDays(3), answered.of("anchor")?.due)
        assertEquals(emptyList<TestCard>(), builder.build(remaining, TestMode.ENGLISH_FIRST))
    }

    @Test
    fun `all modes keep an empty workload empty`() {
        TestMode.entries.forEach { mode ->
            assertEquals(emptyList<TestCard>(), builder.build(Workload(emptyList(), emptyList()), mode))
        }
    }
}
