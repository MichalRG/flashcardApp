package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Slice 1 typed recall still gates a word whose retries now come back-to-back (slice 2). */
class TypedRecallBackToBackRetryTest {
    private val pl = CardDirection.POLISH_FIRST
    private val learning = TestCard(word("anchor"), true, pl)

    @Test
    fun `a lone learning word missed three times requires typing on its fourth prompt then completes`() {
        var difficulty = DirectionalDifficulty.NONE
        var session = TestSession(listOf(learning))

        repeat(3) { index ->
            assertEquals("each repeat occupies a fresh position, so flip and typed state restart", index, session.position)
            assertEquals(learning.key, session.current?.key)
            assertFalse("only the 4th prompt is gated", difficulty.requiresTypedRecall(session.current))
            difficulty = difficulty.recordAnswer("anchor", pl, false)
            session = session.answer(knew = false, timeUp = false)
        }

        assertFalse(session.isOver)
        assertEquals(3, session.position)
        assertEquals(learning.key, session.current?.key)
        assertTrue(difficulty.requiresTypedRecall(session.current))

        difficulty = difficulty.recordAnswer("anchor", pl, true)
        session = session.answer(knew = true, timeUp = false)

        assertTrue(session.isOver)
        assertTrue(completedOf(session))
        assertFalse(difficulty.requiresTypedRecall(learning))
    }

    @Test
    fun `a wrong typed entry recorded as Didn't know keeps the word in the same test`() {
        var difficulty = DirectionalDifficulty.NONE.recordAnswer("anchor", pl, false).recordAnswer("anchor", pl, false)
            .recordAnswer("anchor", pl, false)
        var session = TestSession(listOf(learning)).answer(false, false).answer(false, false).answer(false, false)
        assertTrue(difficulty.requiresTypedRecall(session.current))

        // Continue after a wrong entry records Didn't know through answer(false).
        difficulty = difficulty.recordAnswer("anchor", pl, false)
        session = session.answer(knew = false, timeUp = false)

        assertFalse(session.isOver)
        assertEquals(4, session.position)
        assertTrue(difficulty.requiresTypedRecall(session.current))
    }
}
