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
    fun `zero one or two other cards bring a failed learning card back after all of them`() {
        (0..2).forEach { count ->
            var session = TestSession(listOf(learning) + others(count)).answer(false, false)
            repeat(count) { index ->
                assertFalse(session.isOver)
                assertEquals("other-${index + 1}", session.current?.word?.id)
                session = session.answer(true, false)
            }

            assertFalse("the retry is still owed", session.isOver)
            assertEquals(learning.key, session.current?.key)
            assertTrue(requireNotNull(session.current).isLearning)
            assertFalse(requireNotNull(session.current).isNew)
            assertEquals(count + 1, session.position)

            session = session.answer(true, false)
            assertTrue(session.isOver)
            assertTrue(completedOf(session))
            assertTrue(session.pendingLearning.isEmpty())
            assertEquals(count + 2, session.answered)
        }
    }

    @Test
    fun `replanning separates both the retry and the opposite direction after a failed recall`() {
        val unfinished = learning.copy(isNew = false)
        val opposite = unfinished.copy(direction = CardDirection.ENGLISH_FIRST)
        val session = TestSession.start(
            listOf(unfinished, review("a"), review("b"), opposite) + others(4),
        ).answer(false, false)

        assertEquals(learning.key, session.cards.first().key)
        assertEquals(2, session.cards.count { it.key == learning.key })
        assertTrue(session.cards.any { it.key == opposite.key })
        session.cards.windowed(4, partialWindows = true).forEach { window ->
            assertEquals(window.size, window.map { it.word.id }.distinct().size)
        }
    }

    @Test
    fun `an opposite direction is still shown in the same test after the retry uses the last available gap`() {
        val unfinished = learning.copy(isNew = false)
        val opposite = unfinished.copy(direction = CardDirection.ENGLISH_FIRST)
        val cards = listOf(unfinished, review("a"), review("b"), opposite, review("c"), review("d"))
        var session = TestSession.start(cards).answer(false, false)

        assertEquals(learning.key, session.cards.first().key)
        assertTrue(session.deferredCards.isEmpty())
        assertEquals(2, session.cards.count { it.key == learning.key })
        assertEquals(listOf("anchor", "a", "b", "c", "anchor", "d", "anchor"), session.cards.map { it.word.id })
        assertEquals(opposite.key, session.cards.last().key)
        val retryIndex = session.cards.indices.first { it > 0 && session.cards[it].key == learning.key }
        session.cards.take(retryIndex + 1).windowed(4, partialWindows = true).forEach { window ->
            assertEquals(window.size, window.map { it.word.id }.distinct().size)
        }

        var steps = 0
        while (!session.isOver) {
            assertTrue(steps++ < 20)
            session = session.answer(true, false)
        }
        assertTrue(completedOf(session))
        assertEquals(7, session.answered)
        assertTrue(session.pendingLearning.isEmpty())
    }

    @Test
    fun `both new directions of a single word are shown back-to-back in one test`() {
        val opposite = learning.copy(direction = CardDirection.ENGLISH_FIRST)
        var session = TestSession.start(listOf(learning, opposite)).answer(true, false)

        assertFalse(session.isOver)
        assertEquals(opposite.key, session.current?.key)
        assertEquals(setOf(opposite.key), session.pendingLearning)

        session = session.answer(true, false)
        assertTrue(session.isOver)
        assertTrue(completedOf(session))
        assertEquals(2, session.answered)
        assertTrue(session.pendingLearning.isEmpty())
        assertTrue(session.deferredCards.isEmpty())
        assertEquals(session, session.answer(true, false))
    }

    @Test
    fun `a failed mature review retains the tomorrow schedule policy without an initial-learning retry`() {
        val session = TestSession(listOf(review("anchor")) + others(5)).answer(false, false)

        assertEquals(6, session.cards.size)
        assertTrue(session.pendingLearning.isEmpty())
    }

    @Test
    fun `repeated failures stay spaced while material exists and then return until Knew it`() {
        var session = TestSession(listOf(learning) + others(10))
        val attempts = mutableListOf<Int>()
        while (!session.isOver) {
            assertTrue("The loop must end as soon as Knew it is given", session.answered < 40)
            val isTarget = session.current?.key == learning.key
            if (isTarget) attempts += session.position
            session = session.answer(knew = !isTarget || attempts.size > 5, timeUp = false)
        }

        assertEquals(listOf(0, 5, 10, 13, 14, 15), attempts)
        assertTrue(completedOf(session))
        assertTrue(session.pendingLearning.isEmpty())
        assertEquals(16, session.answered)
        assertEquals(5, session.didNotKnow)
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
