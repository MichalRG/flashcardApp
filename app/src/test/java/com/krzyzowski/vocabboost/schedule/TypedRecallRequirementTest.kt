package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.CardDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedRecallRequirementTest {
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST

    private fun after(vararg answers: Boolean): WordDifficulty =
        answers.fold(WordDifficulty()) { difficulty, knew -> difficulty.answered(knew) }

    private fun misses(id: String, direction: CardDirection, count: Int, from: DirectionalDifficulty = DirectionalDifficulty.NONE) =
        (1..count).fold(from) { difficulty, _ -> difficulty.recordAnswer(id, direction, false) }

    @Test
    fun `a word with no difficulty entry does not require typing`() {
        assertFalse(DirectionalDifficulty.NONE.requiresTypedRecall("boon", en))
        assertFalse(DirectionalDifficulty.NONE.requiresTypedRecall("boon", pl))
    }

    @Test
    fun `zero one and two consecutive misses do not require typing`() {
        assertFalse(WordDifficulty().requiresTypedRecall)
        assertFalse(WordDifficulty(misses = 1).requiresTypedRecall)
        assertFalse(WordDifficulty(misses = 2).requiresTypedRecall)
        (1..2).forEach { count ->
            assertFalse("$count misses", misses("boon", en, count).requiresTypedRecall("boon", en))
        }
    }

    @Test
    fun `three consecutive misses require typing`() {
        assertTrue(WordDifficulty(misses = 3, isDifficult = true).requiresTypedRecall)
        assertTrue(after(false, false, false).requiresTypedRecall)
        assertTrue(misses("boon", en, 3).requiresTypedRecall("boon", en))
    }

    @Test
    fun `a fourth miss keeps typing required because misses stay capped at three`() {
        val fourth = after(false, false, false, false)

        assertEquals(3, fourth.misses)
        assertTrue(fourth.requiresTypedRecall)
        assertTrue(misses("boon", en, 4).requiresTypedRecall("boon", en))
    }

    @Test
    fun `a single correct answer switches typing off for a difficult word`() {
        val recovered = after(false, false, false, true)

        assertTrue(recovered.isDifficult)
        assertEquals(1, recovered.recoveries)
        assertFalse(recovered.requiresTypedRecall)
        val store = misses("boon", en, 3).recordAnswer("boon", en, true)
        assertFalse(store.requiresTypedRecall("boon", en))
    }

    @Test
    fun `a single correct answer switches typing off after a capped fourth miss`() {
        assertFalse(after(false, false, false, false, true).requiresTypedRecall)
    }

    @Test
    fun `a correct answer for a word that is not difficult leaves typing off`() {
        assertFalse(WordDifficulty(misses = 2).answered(true).requiresTypedRecall)
        assertFalse(after(false, false, true).requiresTypedRecall)
    }

    @Test
    fun `a miss after recovery starts the count again and does not require typing`() {
        val restarted = after(false, false, false, true, false)

        assertEquals(1, restarted.misses)
        assertFalse(restarted.requiresTypedRecall)
    }

    @Test
    fun `three more misses after a recovery require typing again`() {
        assertTrue(after(false, false, false, true, false, false, false).requiresTypedRecall)
    }

    @Test
    fun `typing is required only for the direction that has three misses`() {
        val difficulty = misses("boon", en, 3)

        assertTrue(difficulty.requiresTypedRecall("boon", en))
        assertFalse(difficulty.requiresTypedRecall("boon", pl))
    }

    @Test
    fun `two misses in one direction and three in the other require typing only in the second`() {
        val difficulty = misses("boon", pl, 3, from = misses("boon", en, 2))

        assertFalse(difficulty.requiresTypedRecall("boon", en))
        assertTrue(difficulty.requiresTypedRecall("boon", pl))
    }

    @Test
    fun `misses of one word do not require typing for another word`() {
        val difficulty = misses("boon", en, 3)

        assertFalse(difficulty.requiresTypedRecall("bane", en))
    }

    @Test
    fun `a correct answer in the other direction does not clear the requirement`() {
        val difficulty = misses("boon", en, 3).recordAnswer("boon", pl, true)

        assertTrue(difficulty.requiresTypedRecall("boon", en))
    }

    @Test
    fun `legacy progress with three misses requires typing in both directions`() {
        val legacy = (1..3).fold(DifficultyProgress.NONE) { progress, _ -> progress.recordAnswer("boon", false) }
        val difficulty = DirectionalDifficulty.fromLegacy(legacy)

        assertTrue(difficulty.requiresTypedRecall("boon", en))
        assertTrue(difficulty.requiresTypedRecall("boon", pl))
        assertFalse(difficulty.requiresTypedRecall("bane", en))
    }

    @Test
    fun `legacy progress with two misses does not require typing`() {
        val legacy = (1..2).fold(DifficultyProgress.NONE) { progress, _ -> progress.recordAnswer("boon", false) }
        val difficulty = DirectionalDifficulty.fromLegacy(legacy)

        assertFalse(difficulty.requiresTypedRecall("boon", en))
        assertFalse(difficulty.requiresTypedRecall("boon", pl))
    }

    @Test
    fun `the requirement survives encoding and decoding without a new stored field`() {
        val original = misses("boon", en, 3)
        val decoded = DirectionalDifficulty.decode(original.encode())

        assertEquals(original.encode(), decoded.encode())
        assertTrue(decoded.requiresTypedRecall("boon", en))
        assertFalse(decoded.requiresTypedRecall("boon", pl))
    }

    @Test
    fun `existing answer semantics are unchanged`() {
        assertEquals(WordDifficulty(misses = 3, isDifficult = true), after(false, false, false, false))
        assertEquals(WordDifficulty(recoveries = 1, isDifficult = true), after(false, false, false, true))
        assertEquals(WordDifficulty(), after(false, false, false, true, true))
        assertEquals(3, WordDifficulty.MISSES_TO_ENTER)
    }
}
