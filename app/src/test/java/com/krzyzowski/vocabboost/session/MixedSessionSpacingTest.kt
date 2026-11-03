package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixedSessionSpacingTest {
    private fun cards(count: Int, new: Boolean): List<TestCard> {
        val words = (1..count).map { word("word-$it") }
        return if (new) {
            TestMode.MIXED.directions.flatMap { direction -> words.map { TestCard(it, true, direction) } }
        } else {
            words.flatMap { word -> TestMode.MIXED.directions.map { TestCard(word, false, it) } }
        }
    }

    private fun complete(initial: TestSession, failFirst: Boolean = false): Pair<TestSession, List<TestCard>> {
        var session = initial
        val shown = mutableListOf<TestCard>()
        while (!session.isOver) {
            assertTrue("Unexpected endless retry loop", shown.size < 100)
            val knew = !failFirst || shown.isNotEmpty()
            shown += requireNotNull(session.current)
            session = session.answer(knew, timeUp = false)
        }
        shown.windowed(4, partialWindows = true).forEach { window ->
            assertEquals(window.size, window.map { it.word.id }.distinct().size)
        }
        return session to shown
    }

    @Test
    fun `the reported paired-review bug is prevented throughout the whole session`() {
        val original = cards(8, new = false)
        val (session, shown) = complete(TestSession.start(original))

        assertEquals(original.toSet(), shown.toSet())
        assertEquals(16, session.knew)
        assertEquals(0, session.didNotKnow)
        assertTrue(session.deferredCards.isEmpty())
    }

    @Test
    fun `inserting a failed new-word retry cannot put its opposite direction immediately after it`() {
        (4..8).forEach { count ->
            val original = cards(count, new = true)
            val (session, shown) = complete(TestSession.start(original), failFirst = true)

            assertEquals(count * 2 + 1, shown.size)
            assertEquals(1, session.didNotKnow)
            assertTrue(shown.any { it.isLearning && !it.isNew })
            assertTrue(session.pendingLearning.isEmpty())
            assertTrue(session.deferredCards.isEmpty())
        }
    }

    @Test
    fun `short new-word decks retain every deferred learning direction without grading it`() {
        (1..3).forEach { count ->
            val (session, shown) = complete(TestSession.start(cards(count, new = true)))

            assertEquals(count, shown.size)
            assertEquals(count, session.knew)
            assertEquals(count, session.pendingLearning.size)
            assertEquals(session.pendingLearning, session.deferredCards.map { it.key }.toSet())
            assertEquals(session, session.answer(knew = true, timeUp = false))
        }
    }

    @Test
    fun `postponed directions return to the queue when later retries provide enough spacers`() {
        val learning = TestCard(word("a"), false, CardDirection.POLISH_FIRST, isLearning = true)
        val opposite = learning.copy(direction = CardDirection.ENGLISH_FIRST)
        val others = listOf("b", "c", "d").map { learning.copy(word = word(it)) }
        var session = TestSession.start(listOf(learning, opposite) + others).answer(false, false)
        assertEquals(listOf(opposite), session.deferredCards)

        repeat(3) { session = session.answer(false, false) }
        assertTrue(session.deferredCards.isEmpty())
        val (finished, _) = complete(session)

        assertEquals(9, finished.answered)
        assertEquals(4, finished.didNotKnow)
        assertTrue(finished.pendingLearning.isEmpty())
        finished.cards.windowed(4, partialWindows = true).forEach { window ->
            assertEquals(window.size, window.map { it.word.id }.distinct().size)
        }
    }

    @Test
    fun `the timer does not ask to continue when only unspaced directions remain`() {
        val session = TestSession.start(cards(1, new = true)).answer(knew = true, timeUp = true)

        assertTrue(session.isOver)
        assertFalse(session.awaitingTimeChoice)
        assertEquals(1, session.deferredCards.size)
        assertEquals(1, session.pendingLearning.size)
    }

    @Test
    fun `a fixed-direction deck retains its original order when every word is different`() {
        val original = (1..8).map { TestCard(word("word-$it"), false, CardDirection.POLISH_FIRST) }
        val (session, shown) = complete(TestSession.start(original))

        assertEquals(original, shown)
        assertTrue(session.deferredCards.isEmpty())
    }
}
