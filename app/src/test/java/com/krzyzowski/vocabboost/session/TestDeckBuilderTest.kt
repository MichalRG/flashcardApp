package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class TestDeckBuilderTest {
    private val answeredOn = LocalDate.parse("2026-10-01")
    private val ids = (1..40).map { "word-$it" }.toTypedArray()

    private fun reviewsAt(stage: Stage): Pair<Workload, DeckProgress> {
        val list = words(*ids)
        val deck = ids.fold(DeckProgress.NONE) { deck, id -> deck.reaching(id, stage, answeredOn) }
        return Workload(reviews = list, newWords = emptyList()) to deck
    }

    @Test
    fun `reviews below the 7-day step always show the English word first`() {
        listOf(Stage.S1, Stage.S3).forEach { stage ->
            val (workload, deck) = reviewsAt(stage)

            val cards = TestDeckBuilder(Random(7)).build(workload, deck)

            assertEquals("cards at $stage", 40, cards.size)
            val allEnglishFirst = cards.all { it.direction == CardDirection.ENGLISH_FIRST }
            assertTrue("every card at $stage is English first", allEnglishFirst)
        }
    }

    @Test
    fun `reviews from the 7-day step on come up with both the English and the Polish side first`() {
        listOf(Stage.S7, Stage.S14, Stage.S30, Stage.C60, Stage.C90).forEach { stage ->
            val (workload, deck) = reviewsAt(stage)

            val directions = TestDeckBuilder(Random(7)).build(workload, deck).map { it.direction }.toSet()

            val bothSides = setOf(CardDirection.ENGLISH_FIRST, CardDirection.POLISH_FIRST)
            assertEquals("directions at $stage", bothSides, directions)
        }
    }

    @Test
    fun `new words are marked new, show the English word first and follow the reviews in order`() {
        val list = words("anchor", "pearl", "kettle", "lantern")
        val deck = DeckProgress.NONE
            .reaching("anchor", Stage.S3, answeredOn)
            .reaching("pearl", Stage.S3, answeredOn)
        val workload = Workload(reviews = list.take(2), newWords = list.drop(2))

        val cards = TestDeckBuilder(Random(7)).build(workload, deck)

        assertEquals(listOf("anchor", "pearl", "kettle", "lantern"), cards.map { it.word.id })
        assertEquals(listOf(false, false, true, true), cards.map { it.isNew })
        assertEquals(List(4) { CardDirection.ENGLISH_FIRST }, cards.map { it.direction })
    }

    @Test
    fun `an empty workload makes an empty test`() {
        val cards = TestDeckBuilder(Random(7)).build(Workload(emptyList(), emptyList()), DeckProgress.NONE)

        assertEquals(emptyList<TestCard>(), cards)
    }
}
