package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestSessionTest {
    private val cards = listOf("anchor", "pearl", "kettle").map {
        TestCard(word(it), isNew = false, direction = CardDirection.ENGLISH_FIRST)
    }

    @Test
    fun `a test walks the cards in order, counts the answers and ends when none is left`() {
        val session = TestSession(cards)

        val afterFirst = session.answer(knew = true, timeUp = false)
        val afterSecond = afterFirst.answer(knew = false, timeUp = false)
        val afterThird = afterSecond.answer(knew = true, timeUp = false)

        assertEquals("anchor", session.current?.word?.id)
        assertEquals("pearl", afterFirst.current?.word?.id)
        assertEquals("kettle", afterSecond.current?.word?.id)
        assertTrue(afterThird.isOver)
        assertNull(afterThird.current)
        assertEquals(3, afterThird.answered)
        assertEquals(2, afterThird.knew)
        assertEquals(1, afterThird.didNotKnow)
    }

    @Test
    fun `a test with no cards is over at once`() {
        val session = TestSession(emptyList())

        assertTrue(session.isOver)
        assertNull(session.current)
        assertEquals(0, session.answered)
    }

    @Test
    fun `ending the test early keeps the answers given and leaves the other cards unanswered`() {
        val ended = TestSession(cards)
            .answer(knew = true, timeUp = false)
            .answer(knew = false, timeUp = false)
            .end()

        assertTrue(ended.isOver)
        assertNull(ended.current)
        assertEquals(2, ended.answered)
        assertEquals(1, ended.knew)
        assertEquals(1, ended.didNotKnow)
    }

    @Test
    fun `the card on screen can be answered at ten minutes before choosing stop or continue`() {
        val session = TestSession(cards).answer(knew = true, timeUp = false)

        assertFalse(session.isOver)
        val lastAnswer = session.answer(knew = false, timeUp = true)

        assertFalse(lastAnswer.isOver)
        assertTrue(lastAnswer.awaitingTimeChoice)
        assertEquals("kettle", lastAnswer.current?.word?.id)
        assertEquals(2, lastAnswer.answered)
        assertEquals(1, lastAnswer.didNotKnow)
        assertTrue(lastAnswer.end().isOver)
        assertTrue(lastAnswer.continueLearning().answer(knew = true, timeUp = true).isOver)
    }

    @Test
    fun `an answer after the test is over changes nothing`() {
        val ended = TestSession(cards).answer(knew = true, timeUp = false).end()

        val after = ended.answer(knew = true, timeUp = false)

        assertEquals(ended, after)
    }
}
