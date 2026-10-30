package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PracticeDeckTest {
    @Test
    fun `practice offers every word in list order with its stage, including new and retired ones`() {
        val list = words("puddle", "boon", "thorough", "bargain", "reluctant")
        val answeredOn = LocalDate.parse("2026-10-01")
        val progress = DeckProgress.NONE
            .reaching("boon", Stage.S1, answeredOn)
            .reaching("thorough", Stage.C60, answeredOn)
            .reaching("bargain", Stage.RETIRED, answeredOn)

        val deck = PracticeDeck(list, progress)

        assertEquals(5, deck.size)
        val inListOrder = (0 until deck.size).map { deck.wordAt(it).id }
        assertEquals(listOf("puddle", "boon", "thorough", "bargain", "reluctant"), inListOrder)
        assertNull(deck.stageAt(0))
        assertEquals(Stage.S1, deck.stageAt(1))
        assertEquals(Stage.C60, deck.stageAt(2))
        assertEquals(Stage.RETIRED, deck.stageAt(3))
        assertNull(deck.stageAt(4))
    }
}
