package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PracticeDeckTest {
    private fun difficult(vararg ids: String): DifficultyProgress =
        ids.fold(DifficultyProgress.NONE) { progress, id ->
            (1..3).fold(progress) { current, _ -> current.recordAnswer(id, knew = false) }
        }

    @Test
    fun `practice offers every word in list order with its stage including new and maintenance ones`() {
        val list = words("puddle", "boon", "thorough", "bargain", "reluctant")
        val answeredOn = LocalDate.parse("2026-10-01")
        val progress = DeckProgress.NONE
            .reaching("boon", Stage.S1, answeredOn)
            .reaching("thorough", Stage.C60, answeredOn)
            .reaching("bargain", Stage.MAINTENANCE, answeredOn)

        val deck = PracticeDeck(list, progress)

        assertEquals(5, deck.size)
        val inListOrder = (0 until deck.size).map { deck.wordAt(it).id }
        assertEquals(listOf("puddle", "boon", "thorough", "bargain", "reluctant"), inListOrder)
        assertNull(deck.stageAt(0))
        assertEquals(Stage.S1, deck.stageAt(1))
        assertEquals(Stage.C60, deck.stageAt(2))
        assertEquals(Stage.MAINTENANCE, deck.stageAt(3))
        assertNull(deck.stageAt(4))
    }

    @Test
    fun `difficult practice offers only flagged words in vocabulary order with their existing stages`() {
        val list = words("puddle", "boon", "thorough", "bargain")
        val day = LocalDate.parse("2026-11-02")
        val progress = DeckProgress.NONE
            .reaching("puddle", Stage.S1, day)
            .reaching("bargain", Stage.S3, day)
        val difficulty = difficult("bargain", "puddle", "removed-word").recordAnswer("boon", knew = false)

        val deck = PracticeDeck.difficult(list, progress, difficulty)

        assertEquals(2, deck.size)
        assertEquals(listOf("puddle", "bargain"), (0 until deck.size).map { deck.wordAt(it).id })
        assertEquals(Stage.S1, deck.stageAt(0))
        assertEquals(Stage.S3, deck.stageAt(1))
        assertEquals(4, PracticeDeck(list, progress).size)
    }

    @Test
    fun `the difficult deck is empty before tracking begins and after two successful recoveries`() {
        val list = words("boon")
        val recovered = difficult("boon")
            .recordAnswer("boon", knew = true)
            .recordAnswer("boon", knew = true)

        assertEquals(0, PracticeDeck.difficult(list, DeckProgress.NONE, DifficultyProgress.NONE).size)
        assertEquals(0, PracticeDeck.difficult(list, DeckProgress.NONE, recovered).size)
    }

    @Test
    fun `one successful recovery still leaves the word available for extra practice`() {
        val difficulty = difficult("boon").recordAnswer("boon", knew = true)
        val deck = PracticeDeck.difficult(words("boon"), DeckProgress.NONE, difficulty)

        assertEquals(1, deck.size)
        assertEquals("boon", deck.wordAt(0).id)
    }

    @Test
    fun `difficulty follows stable ids through corrected removed and readded vocabulary`() {
        val saved = DifficultyProgress.decode(difficult("boon").encode())
        val corrected = word("boon", english = "boon (noun)")

        val removed = PracticeDeck.difficult(words("puddle"), DeckProgress.NONE, saved)
        val restored = PracticeDeck.difficult(listOf(corrected), DeckProgress.NONE, saved)

        assertEquals(0, removed.size)
        assertEquals(corrected, restored.wordAt(0))
    }

    @Test
    fun `positions remain in bounds when the difficult list shrinks or becomes empty`() {
        val before = PracticeDeck.difficult(words("puddle", "boon"), DeckProgress.NONE, difficult("puddle", "boon"))
        val after = PracticeDeck.difficult(words("puddle"), DeckProgress.NONE, difficult("puddle", "boon"))
        val empty = PracticeDeck.difficult(emptyList(), DeckProgress.NONE, DifficultyProgress.NONE)

        assertEquals(0, before.boundedPosition(-1))
        assertEquals(1, before.boundedPosition(1))
        assertEquals(1, before.boundedPosition(10))
        assertEquals(0, after.boundedPosition(1))
        assertEquals(0, empty.boundedPosition(1))
    }
}
