package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningSessionTest {
    private val learning = TestCard(word("anchor"), true, CardDirection.POLISH_FIRST)
    private fun review(id: String) = TestCard(word(id), false, CardDirection.ENGLISH_FIRST)
    private fun others(count: Int) = (1..count).map { review("other-$it") }

    @Test
    fun `a failed initial card retries after four intervening cards`() {
        val initial = TestSession(listOf(learning) + others(6))
        var session = initial.answer(knew = false, timeUp = false)

        assertEquals(listOf("anchor", "other-1", "other-2", "other-3", "other-4", "anchor", "other-5", "other-6"),
            session.cards.map { it.word.id })
        repeat(4) { session = session.answer(knew = true, timeUp = false) }

        assertEquals(learning.key, session.current?.key)
        assertTrue(requireNotNull(session.current).isLearning)
        assertFalse(requireNotNull(session.current).isNew)
        assertEquals(7, initial.cards.size)
        assertEquals(setOf(learning.key), session.pendingLearning)

        session = session.answer(knew = true, timeUp = false)
        assertTrue(session.pendingLearning.isEmpty())
        assertEquals(1, session.didNotKnow)
        assertEquals(5, session.knew)
    }

    @Test
    fun `three intervening cards are enough when four are not available`() {
        val session = TestSession(listOf(learning) + others(3)).answer(false, false)

        assertEquals(listOf("anchor", "other-1", "other-2", "other-3", "anchor"), session.cards.map { it.word.id })
    }

    @Test
    fun `zero one or two other cards defer failed learning rather than repeat it immediately`() {
        (0..2).forEach { count ->
            var session = TestSession(listOf(learning) + others(count)).answer(false, false)
            repeat(count) { session = session.answer(true, false) }

            assertTrue(session.isOver)
            assertEquals(count + 1, session.answered)
            assertEquals(count + 1, session.cards.size)
            assertEquals(setOf(learning.key), session.pendingLearning)
        }
    }

    @Test
    fun `showing the opposite direction resets the retry gap for the same word`() {
        val opposite = learning.copy(direction = CardDirection.ENGLISH_FIRST)
        val session = TestSession(
            listOf(learning, review("a"), review("b"), opposite) + others(4),
        ).answer(false, false)

        assertEquals(learning.key, session.cards.last().key)
        assertEquals(listOf("other-1", "other-2", "other-3", "other-4"),
            session.cards.takeLast(5).dropLast(1).map { it.word.id })
    }

    @Test
    fun `other cards on both sides of an opposite-direction prompt do not combine into a retry gap`() {
        val opposite = learning.copy(direction = CardDirection.ENGLISH_FIRST)
        val cards = listOf(learning, review("a"), review("b"), opposite, review("c"), review("d"))
        val session = TestSession(cards).answer(false, false)

        assertEquals(cards, session.cards)
    }

    @Test
    fun `a single correct recall completes only the attempted direction`() {
        val opposite = learning.copy(direction = CardDirection.ENGLISH_FIRST)
        val session = TestSession(listOf(learning, opposite)).answer(true, false)

        assertEquals(setOf(opposite.key), session.pendingLearning)
        assertEquals(2, session.cards.size)
        assertTrue(session.answer(true, false).pendingLearning.isEmpty())
    }

    @Test
    fun `a failed mature review retains the tomorrow schedule policy without an initial-learning retry`() {
        val session = TestSession(listOf(review("anchor")) + others(5)).answer(false, false)

        assertEquals(6, session.cards.size)
        assertTrue(session.pendingLearning.isEmpty())
    }

    @Test
    fun `repeated failures have spaced attempts and do not create an immediate end-of-deck loop`() {
        var session = TestSession(listOf(learning) + others(10))
        val attempts = mutableListOf<Int>()
        while (!session.isOver) {
            val isTarget = session.current?.key == learning.key
            if (isTarget) attempts += session.position
            session = session.answer(knew = !isTarget, timeUp = false)
            assertTrue("A finite deck must not produce an endless retry loop", session.answered < 30)
        }

        assertEquals(listOf(0, 5, 10), attempts)
        assertEquals(setOf(learning.key), session.pendingLearning)
    }

    @Test
    fun `ten minutes pauses for a choice instead of discarding queued learning`() {
        val session = TestSession(listOf(learning) + others(4)).answer(false, true)

        assertTrue(session.awaitingTimeChoice)
        assertFalse(session.isOver)
        assertEquals(6, session.cards.size)
        assertEquals(session, session.answer(true, true))
        assertTrue(session.end().isOver)
        assertEquals(setOf(learning.key), session.end().pendingLearning)
    }

    @Test
    fun `continuing after ten minutes does not repeatedly prompt or stop the session`() {
        var session = TestSession(listOf(learning) + others(4)).answer(false, true).continueLearning()
        while (!session.isOver) {
            session = session.answer(true, true)
            assertFalse(session.awaitingTimeChoice)
        }

        assertTrue(session.continuedPastLimit)
        assertEquals(6, session.answered)
        assertTrue(session.pendingLearning.isEmpty())
    }

    @Test
    fun `the last answer finishes normally without an unnecessary ten-minute choice`() {
        val session = TestSession(listOf(learning)).answer(true, true)

        assertTrue(session.isOver)
        assertFalse(session.awaitingTimeChoice)
        assertTrue(session.pendingLearning.isEmpty())
    }
}
