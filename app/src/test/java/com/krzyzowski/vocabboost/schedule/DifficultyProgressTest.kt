package com.krzyzowski.vocabboost.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyProgressTest {
    private val golden = "boon=0;1;true,puddle=2;0;false"

    @Test
    fun `miss and recovery streaks read back from saved text`() {
        val progress = DifficultyProgress.decode(golden)

        assertEquals(WordDifficulty(recoveries = 1, isDifficult = true), progress.of("boon"))
        assertEquals(WordDifficulty(misses = 2), progress.of("puddle"))
        assertTrue(progress.isDifficult("boon"))
        assertFalse(progress.isDifficult("puddle"))
        assertNull(progress.of("unseen"))
        assertFalse(progress.isDifficult("unseen"))
    }

    @Test
    fun `saved text is deterministic and sorted by stable word id`() {
        var progress = DifficultyProgress.NONE
        repeat(2) { progress = progress.recordAnswer("puddle", knew = false) }
        repeat(3) { progress = progress.recordAnswer("boon", knew = false) }
        progress = progress.recordAnswer("boon", knew = true)

        assertEquals(golden, progress.encode())
        assertEquals(golden, DifficultyProgress.decode(progress.encode()).encode())
    }

    @Test
    fun `recovery continues after saving and removes only the recovered word`() {
        val before = DifficultyProgress.decode(golden)
        val after = before.recordAnswer("boon", knew = true)

        assertNull(after.of("boon"))
        assertFalse(after.isDifficult("boon"))
        assertEquals("puddle=2;0;false", after.encode())
        assertEquals(golden, before.encode())
    }

    @Test
    fun `answers for another word neither increment nor break a word's streak`() {
        val progress = DifficultyProgress.NONE
            .recordAnswer("boon", knew = false)
            .recordAnswer("puddle", knew = false)
            .recordAnswer("boon", knew = false)
            .recordAnswer("puddle", knew = true)

        assertEquals(WordDifficulty(misses = 2), progress.of("boon"))
        assertNull(progress.of("puddle"))
        assertTrue(progress.recordAnswer("boon", knew = false).isDifficult("boon"))
    }

    @Test
    fun `a correct answer for an untracked word needs no stored entry`() {
        assertEquals("", DifficultyProgress.NONE.recordAnswer("boon", knew = true).encode())
    }

    @Test
    fun `empty text represents an empty difficult list`() {
        val progress = DifficultyProgress.decode("")

        assertEquals("", progress.encode())
        assertFalse(progress.isDifficult("boon"))
    }

    @Test
    fun `malformed difficulty data is rejected instead of silently resetting history`() {
        listOf(
            "no-equals-sign",
            "=1;0;false",
            "boon=1;0",
            "boon=3;0;maybe",
            "boon=-1;0;false",
            "boon=4;0;true",
            "boon=3;0;false",
            "boon=0;2;true",
            "boon=1;1;true",
            "boon=1;0;false,boon=2;0;false",
        ).forEach { text ->
            assertThrows(text, IllegalArgumentException::class.java) { DifficultyProgress.decode(text) }
        }
    }
}
