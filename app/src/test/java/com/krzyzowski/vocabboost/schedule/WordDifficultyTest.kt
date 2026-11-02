package com.krzyzowski.vocabboost.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDifficultyTest {
    private fun answers(vararg knew: Boolean): WordDifficulty =
        knew.fold(WordDifficulty()) { difficulty, answer -> difficulty.answered(answer) }

    @Test
    fun `only the third consecutive miss makes a word difficult`() {
        val first = answers(false)
        val second = first.answered(false)
        val third = second.answered(false)

        assertEquals(1, first.misses)
        assertFalse(first.isDifficult)
        assertEquals(2, second.misses)
        assertFalse(second.isDifficult)
        assertEquals(3, third.misses)
        assertTrue(third.isDifficult)
    }

    @Test
    fun `further misses keep the word difficult without an unbounded counter`() {
        val difficulty = List(100) { false }.fold(WordDifficulty()) { state, knew -> state.answered(knew) }

        assertEquals(WordDifficulty(misses = 3, isDifficult = true), difficulty)
    }

    @Test
    fun `a correct answer before entry resets the miss streak`() {
        val difficulty = answers(false, false, true, false)

        assertEquals(WordDifficulty(misses = 1), difficulty)
    }

    @Test
    fun `one correct answer is not enough to leave the difficult list`() {
        val difficulty = answers(false, false, false, true)

        assertEquals(0, difficulty.misses)
        assertEquals(1, difficulty.recoveries)
        assertTrue(difficulty.isDifficult)
    }

    @Test
    fun `the second consecutive correct answer clears difficulty and both streaks`() {
        assertEquals(WordDifficulty(), answers(false, false, false, true, true))
    }

    @Test
    fun `a miss interrupts recovery but does not remove a difficult word`() {
        val interrupted = answers(false, false, false, true, false)

        assertEquals(WordDifficulty(misses = 1, isDifficult = true), interrupted)
        assertTrue(interrupted.answered(true).isDifficult)
        assertEquals(WordDifficulty(), interrupted.answered(true).answered(true))
    }

    @Test
    fun `a recovered word needs three fresh consecutive misses to reenter`() {
        val recovered = answers(false, false, false, true, true)
        val first = recovered.answered(false)
        val second = first.answered(false)

        assertFalse(first.isDifficult)
        assertFalse(second.isDifficult)
        assertTrue(second.answered(false).isDifficult)
    }

    @Test
    fun `correct answers for an ordinary word need no saved recovery state`() {
        assertEquals(WordDifficulty(), answers(true, true, true))
    }

    @Test
    fun `invalid persisted streak combinations fail explicitly`() {
        listOf(
            { WordDifficulty(misses = -1) },
            { WordDifficulty(misses = 4, isDifficult = true) },
            { WordDifficulty(recoveries = -1, isDifficult = true) },
            { WordDifficulty(recoveries = 2, isDifficult = true) },
            { WordDifficulty(misses = 1, recoveries = 1, isDifficult = true) },
            { WordDifficulty(misses = 3) },
            { WordDifficulty(recoveries = 1) },
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { invalid() }
        }
    }
}
