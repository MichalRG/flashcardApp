package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedRecallLiveRequirementTest {
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST
    private val learning = TestCard(word("anchor"), true, pl)
    private fun review(id: String) = TestCard(word(id), false, en)
    private fun others(count: Int) = (1..count).map { review("other-$it") }

    private fun misses(count: Int, from: DirectionalDifficulty = DirectionalDifficulty.NONE) =
        (1..count).fold(from) { difficulty, _ -> difficulty.recordAnswer("anchor", pl, false) }

    @Test
    fun `no card and no difficulty data never require typing`() {
        val loaded: DirectionalDifficulty = misses(3)

        assertFalse(loaded.requiresTypedRecall(null))
        assertFalse((null as DirectionalDifficulty?).requiresTypedRecall(learning))
        assertFalse((null as DirectionalDifficulty?).requiresTypedRecall(null))
    }

    @Test
    fun `a card requires typing only in the direction with three misses`() {
        val difficulty = misses(3)

        assertTrue(difficulty.requiresTypedRecall(learning))
        assertTrue(difficulty.requiresTypedRecall(learning.copy(isNew = false)))
        assertFalse(difficulty.requiresTypedRecall(TestCard(word("anchor"), false, en)))
        assertFalse(difficulty.requiresTypedRecall(review("other")))
    }

    @Test
    fun `a learning card missed up to three times requires typing on its retry in the same session`() {
        var difficulty = misses(2)
        var session = TestSession(listOf(learning) + others(6))

        assertEquals(learning.key, session.current?.key)
        assertFalse("two misses so far", difficulty.requiresTypedRecall(session.current))

        difficulty = difficulty.recordAnswer("anchor", pl, false)
        session = session.answer(knew = false, timeUp = false)
        repeat(4) {
            assertFalse(difficulty.requiresTypedRecall(session.current))
            session = session.answer(knew = true, timeUp = false)
        }

        assertEquals(learning.key, session.current?.key)
        assertTrue("retry copy must be gated", difficulty.requiresTypedRecall(session.current))
    }

    @Test
    fun `a retry whose streak dropped below three does not require typing`() {
        var difficulty = misses(3)
        var session = TestSession(listOf(learning) + others(6))
        assertTrue(difficulty.requiresTypedRecall(session.current))

        difficulty = difficulty.recordAnswer("anchor", pl, true)
        session = session.answer(knew = true, timeUp = false)

        assertFalse(difficulty.requiresTypedRecall(learning))
        assertFalse(difficulty.requiresTypedRecall(session.current))
    }

    @Test
    fun `typing is no longer required after a recorded knew it`() {
        val gated = misses(3)

        assertTrue(gated.requiresTypedRecall(learning))
        assertFalse(gated.recordAnswer("anchor", pl, true).requiresTypedRecall(learning))
    }

    @Test
    fun `the requirement is read from difficulty and does not change the card or the session API`() {
        val card = TestCard(word("anchor"), true, pl)
        val session = TestSession(listOf(card) + others(3))

        assertEquals(TestCard(word("anchor"), true, pl, true), card)
        assertEquals(card, session.current)
        assertEquals(1, session.answer(knew = true, timeUp = false).position)
    }
}
