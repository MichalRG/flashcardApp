package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Slice 2: a test is completed only when nothing is left to do in it. */
class SessionCompletionContractTest {
    private val learning = TestCard(word("anchor"), true, CardDirection.POLISH_FIRST)
    private fun review(id: String, direction: CardDirection = CardDirection.ENGLISH_FIRST) =
        TestCard(word(id), false, direction)
    private fun others(count: Int) = (1..count).map { review("other-$it") }

    private fun bothDirections(id: String, new: Boolean) =
        listOf(CardDirection.POLISH_FIRST, CardDirection.ENGLISH_FIRST).map { TestCard(word(id), new, it) }

    // AC1 / AC2

    @Test
    fun `remaining counts the card on screen and every queued card`() {
        val session = TestSession(others(3))

        assertEquals(3, remainingOf(session))
        assertFalse(completedOf(session))
        assertEquals(2, remainingOf(session.answer(true, false)))
        assertEquals(0, remainingOf(session.answer(true, false).answer(true, false).answer(true, false)))
    }

    @Test
    fun `a learning key is not counted twice on top of its queued retry`() {
        val session = TestSession(listOf(learning) + others(2))
        assertEquals(3, remainingOf(session))

        val missed = session.answer(knew = false, timeUp = false)

        assertEquals("other-1, other-2 and the retry", 3, remainingOf(missed))
        assertEquals(setOf(learning.key), missed.pendingLearning)
    }

    @Test
    fun `an unstopped session with work left is never over and always has a current card`() {
        (0..2).forEach { count ->
            val session = TestSession(listOf(learning) + others(count)).answer(false, false)
            var step = session
            repeat(count) {
                assertFalse(step.isOver)
                assertNotNull(step.current)
                assertTrue(remainingOf(step) > 0)
                step = step.answer(true, false)
            }
            assertFalse("the retry is still owed", step.isOver)
            assertNotNull(step.current)
        }
    }

    @Test
    fun `completed means nothing remains whether or not the test was stopped`() {
        val done = TestSession(others(1)).answer(true, false)

        assertTrue(done.isOver)
        assertTrue(completedOf(done))
        assertEquals(0, remainingOf(done))
        assertTrue(completedOf(done.end()))
    }

    @Test
    fun `an answer after the test is over changes nothing even with leftovers`() {
        val ended = TestSession(listOf(learning) + others(1)).answer(false, false).end()

        assertTrue(ended.isOver)
        assertEquals(ended, ended.answer(true, false))
    }

    // AC3

    @Test
    fun `a failed learning card with no room comes straight back when nothing else is left`() {
        val session = TestSession(listOf(learning)).answer(knew = false, timeUp = false)

        assertFalse(session.isOver)
        assertFalse(completedOf(session))
        assertEquals(1, session.position)
        assertEquals(learning.key, session.current?.key)
        assertTrue(requireNotNull(session.current).isLearning)
        assertFalse(requireNotNull(session.current).isNew)

        val done = session.answer(knew = true, timeUp = false)
        assertTrue(done.isOver)
        assertTrue(completedOf(done))
        assertTrue(done.pendingLearning.isEmpty())
        assertEquals(2, done.answered)
    }

    // AC4

    @Test
    fun `a word that keeps failing keeps coming back until Knew it with no cap`() {
        (0..2).forEach { count ->
            var session = TestSession(listOf(learning) + others(count))
            var misses = 0
            var steps = 0
            while (misses < 50) {
                assertTrue("runaway loop", steps++ < 500)
                assertFalse("not over while the learning word has not been known", session.isOver)
                val isTarget = session.current?.key == learning.key
                if (count == 0) assertTrue("only the target can be shown", isTarget)
                if (isTarget) misses++
                session = session.answer(knew = !isTarget, timeUp = false)
            }
            // The last miss queued one more retry; every other card was answered once.
            assertFalse(session.isOver)
            assertEquals(50, session.didNotKnow)
            assertEquals(setOf(learning.key), session.pendingLearning)
            while (session.current?.key != learning.key) {
                assertTrue("runaway loop", steps++ < 500)
                session = session.answer(true, false)
            }

            session = session.answer(knew = true, timeUp = false)

            assertTrue(session.isOver)
            assertTrue(completedOf(session))
            assertTrue(session.pendingLearning.isEmpty())
            assertEquals(50, session.didNotKnow)
            assertEquals(count + 1, session.knew)
        }
    }

    @Test
    fun `back-to-back repeats of the same card occupy new positions`() {
        var session = TestSession(listOf(learning))
        repeat(10) { index ->
            assertEquals(index, session.position)
            assertEquals(learning.key, session.current?.key)
            session = session.answer(knew = false, timeUp = false)
        }
        assertEquals(10, session.position)
        assertEquals(learning.key, session.current?.key)
    }

    // AC6

    @Test
    fun `both directions of one new word are shown back to back in the same test`() {
        val deck = bothDirections("solo", new = true)
        var session = TestSession.start(deck)
        val first = requireNotNull(session.current)

        session = session.answer(knew = true, timeUp = false)

        assertFalse(session.isOver)
        assertTrue(session.deferredCards.isEmpty())
        val second = requireNotNull(session.current)
        assertEquals(first.word.id, second.word.id)
        assertTrue(first.direction != second.direction)
        assertEquals(setOf(second.key), session.pendingLearning)

        session = session.answer(knew = true, timeUp = false)
        assertTrue(session.isOver)
        assertTrue(completedOf(session))
        assertEquals(2, session.answered)
        assertTrue(session.pendingLearning.isEmpty())
    }

    @Test
    fun `both directions of one mature word are shown back to back in the same test`() {
        var session = TestSession.start(bothDirections("solo", new = false))
        val first = requireNotNull(session.current)

        session = session.answer(knew = true, timeUp = false)

        assertFalse(session.isOver)
        val second = requireNotNull(session.current)
        assertEquals(first.word.id, second.word.id)
        assertTrue(first.direction != second.direction)

        session = session.answer(knew = true, timeUp = false)
        assertTrue(session.isOver)
        assertTrue(completedOf(session))
        assertEquals(2, session.answered)
        assertTrue(session.deferredCards.isEmpty())
    }

    // AC11 / AC20

    @Test
    fun `ending early with work left is not complete and reports what is still owed`() {
        val session = TestSession(listOf(learning) + others(2))
            .answer(knew = false, timeUp = false)
        val ended = session.end()

        assertTrue(ended.isOver)
        assertNull(ended.current)
        assertFalse(completedOf(ended))
        assertEquals(3, remainingOf(ended))
        assertEquals(session.knew, ended.knew)
        assertEquals(session.didNotKnow, ended.didNotKnow)
        assertEquals(session.answered, ended.answered)
    }

    @Test
    fun `ending a test that has not started leaves every card owed`() {
        val ended = TestSession(others(4)).end()

        assertTrue(ended.isOver)
        assertFalse(completedOf(ended))
        assertEquals(4, remainingOf(ended))
        assertEquals(0, ended.answered)
    }

    @Test
    fun `ending after the very last answer is still completed`() {
        val last = TestSession(others(2)).answer(true, false).answer(false, false)

        assertTrue(completedOf(last.end()))
        assertEquals(0, remainingOf(last.end()))
    }

    @Test
    fun `an empty test is completed at once`() {
        listOf(TestSession(emptyList()), TestSession.start(emptyList())).forEach { session ->
            assertTrue(session.isOver)
            assertTrue(completedOf(session))
            assertEquals(0, remainingOf(session))
            assertNull(session.current)
        }
    }

    // AC12 / AC13

    @Test
    fun `at ten minutes a leftover retry still asks Stop or Continue`() {
        val session = TestSession(listOf(learning)).answer(knew = false, timeUp = true)

        assertTrue(session.awaitingTimeChoice)
        assertFalse(session.isOver)
        assertEquals(1, remainingOf(session))
        assertEquals(session, session.answer(true, true))
        assertEquals(learning.key, session.continueLearning().current?.key)
    }

    @Test
    fun `at ten minutes a formerly held-back direction asks Stop or Continue and can be continued`() {
        val session = TestSession.start(bothDirections("solo", new = true)).answer(knew = true, timeUp = true)

        assertTrue(session.awaitingTimeChoice)
        assertFalse(session.isOver)
        assertEquals(1, remainingOf(session))

        val continued = session.continueLearning()
        assertFalse(continued.awaitingTimeChoice)
        assertTrue(continued.continuedPastLimit)
        val finished = continued.answer(knew = true, timeUp = true)
        assertTrue(finished.isOver)
        assertTrue(completedOf(finished))
        assertFalse(finished.awaitingTimeChoice)
    }

    @Test
    fun `stopping at the ten minute choice is not complete`() {
        val prompt = TestSession.start(bothDirections("solo", new = true)).answer(knew = true, timeUp = true)

        val ended = prompt.end()

        assertTrue(ended.isOver)
        assertFalse(completedOf(ended))
        assertEquals(1, remainingOf(ended))
    }

    @Test
    fun `after continuing past ten minutes later answers never prompt again`() {
        var session = TestSession.start((1..2).flatMap { bothDirections("word-$it", new = false) })
            .answer(knew = true, timeUp = true)
        assertTrue(session.awaitingTimeChoice)
        session = session.continueLearning()
        var steps = 0
        while (!session.isOver) {
            assertTrue(steps++ < 20)
            session = session.answer(knew = true, timeUp = true)
            assertFalse(session.awaitingTimeChoice)
        }
        assertTrue(completedOf(session))
        assertEquals(4, session.answered)
    }

    @Test
    fun `an answer at ten minutes that leaves nothing finishes without a choice`() {
        val session = TestSession(listOf(learning)).answer(knew = true, timeUp = true)

        assertTrue(session.isOver)
        assertTrue(completedOf(session))
        assertFalse(session.awaitingTimeChoice)
    }
}
