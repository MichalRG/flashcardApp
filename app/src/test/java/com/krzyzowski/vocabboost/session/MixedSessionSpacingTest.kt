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

    private fun completeBestEffort(initial: TestSession): Pair<TestSession, List<TestCard>> {
        var session = initial
        val shown = mutableListOf<TestCard>()
        while (!session.isOver) {
            assertTrue("Unexpected endless retry loop", shown.size < 100)
            shown += requireNotNull(session.current)
            session = session.answer(knew = true, timeUp = false)
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
    fun `short mixed decks show every direction in the same test with the largest gap available`() {
        (1..3).forEach { count ->
            listOf(true, false).forEach { new ->
                val original = cards(count, new)
                val (session, shown) = completeBestEffort(TestSession.start(original))

                assertEquals(original.size, shown.size)
                assertEquals(original.toSet(), shown.toSet())
                assertEquals(original.size, session.knew)
                assertTrue(session.isOver)
                assertTrue(completedOf(session))
                assertTrue(session.pendingLearning.isEmpty())
                assertTrue(session.deferredCards.isEmpty())
                (1..count).forEach { index ->
                    val positions = shown.indices.filter { shown[it].word.id == "word-$index" }
                    assertEquals(2, positions.size)
                    assertEquals(
                        "word-$index of $count words (new=$new) has the most spacing the deck allows",
                        count - 1,
                        positions.last() - positions.first() - 1,
                    )
                }
            }
        }
    }

    @Test
    fun `postponed directions return to the queue when later retries provide enough spacers`() {
        val learning = TestCard(word("a"), false, CardDirection.POLISH_FIRST, isLearning = true)
        val opposite = learning.copy(direction = CardDirection.ENGLISH_FIRST)
        val others = listOf("b", "c", "d").map { learning.copy(word = word(it)) }
        var session = TestSession.start(listOf(learning, opposite) + others).answer(false, false)
        assertTrue(session.deferredCards.isEmpty())
        assertEquals(opposite.key, session.cards.last().key)
        assertEquals(6, session.cards.size)

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
    fun `the timer asks to continue while a formerly held-back direction remains`() {
        val session = TestSession.start(cards(1, new = true)).answer(knew = true, timeUp = true)

        assertTrue(session.awaitingTimeChoice)
        assertFalse(session.isOver)
        assertEquals(1, remainingOf(session))
        assertEquals(1, session.pendingLearning.size)
        assertTrue(session.deferredCards.isEmpty())
    }

    @Test
    fun `a fixed-direction deck retains its original order when every word is different`() {
        val original = (1..8).map { TestCard(word("word-$it"), false, CardDirection.POLISH_FIRST) }
        val (session, shown) = complete(TestSession.start(original))

        assertEquals(original, shown)
        assertTrue(session.deferredCards.isEmpty())
    }
}
