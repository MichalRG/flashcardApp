package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.CardDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DirectionalProgressTest {
    private val today = LocalDate.parse("2026-11-02")
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST

    @Test
    fun `legacy stages dates and retirement are copied into both directions without resetting`() {
        val legacy = DeckProgress.decode("boon=S7;2026-11-05;2026-10-20,puddle=RET;-;2026-03-01")
        val migrated = DirectionalProgress.fromLegacy(legacy)

        CardDirection.entries.forEach { direction ->
            assertEquals(legacy.encode(), migrated.forDirection(direction).schedule.encode())
            assertFalse(migrated.isLearning("boon", direction))
        }
        assertEquals(migrated.encode(), DirectionalProgress.decode(migrated.encode()).encode())
    }

    @Test
    fun `a first correct answer completes only that direction and schedules tomorrow`() {
        val deck = DirectionalProgress.NONE.recordAnswer("boon", pl, true, today)

        assertEquals(Stage.S1, deck.of("boon", pl)?.stage)
        assertEquals(today.plusDays(1), deck.of("boon", pl)?.due)
        assertFalse(deck.isLearning("boon", pl))
        assertTrue(deck.isLearning("boon", en))
        assertEquals(1, deck.introducedOn(today))
    }

    @Test
    fun `failed initial learning needs a correct recall and does not advance to the three-day stage`() {
        var deck = DirectionalProgress.NONE
        repeat(5) { deck = deck.recordAnswer("boon", en, false, today) }
        val saved = DirectionalProgress.decode(deck.encode())
        val answered = saved.recordAnswer("boon", en, true, today.plusDays(4))

        assertTrue(saved.isLearning("boon", en))
        assertFalse(answered.isLearning("boon", en))
        assertTrue(answered.isLearning("boon", pl))
        assertEquals(Stage.S1, answered.of("boon", en)?.stage)
        assertEquals(today.plusDays(5), answered.of("boon", en)?.due)
        assertEquals(today, answered.of("boon", en)?.introducedOn)
        assertEquals(1, answered.introducedOn(today))
        assertEquals(0, answered.introducedOn(today.plusDays(4)))
    }

    @Test
    fun `completing both directions counts one distinct new word`() {
        val deck = DirectionalProgress.NONE
            .recordAnswer("boon", en, true, today)
            .recordAnswer("boon", pl, true, today)

        assertFalse(deck.isLearning("boon", en))
        assertFalse(deck.isLearning("boon", pl))
        assertEquals(Stage.S1, deck.of("boon", en)?.stage)
        assertEquals(Stage.S1, deck.of("boon", pl)?.stage)
        assertEquals(1, deck.introducedOn(today))
    }

    @Test
    fun `later successful and failed reviews change only their own direction`() {
        val legacy = DeckProgress.decode("boon=S7;2026-11-02;2026-10-20")
        val original = DirectionalProgress.fromLegacy(legacy)
        val answered = original.recordAnswer("boon", pl, false, today).recordAnswer("boon", en, true, today)

        assertEquals(Stage.S1, answered.of("boon", pl)?.stage)
        assertEquals(today.plusDays(1), answered.of("boon", pl)?.due)
        assertEquals(Stage.S14, answered.of("boon", en)?.stage)
        assertEquals(today.plusDays(14), answered.of("boon", en)?.due)
        assertEquals(Stage.S7, original.of("boon", en)?.stage)
    }

    @Test
    fun `pending directions and divergent schedules survive serialization`() {
        val deck = DirectionalProgress.NONE.recordAnswer("boon", pl, true, today)
            .recordAnswer("puddle", en, false, today)
        val saved = DirectionalProgress.decode(deck.encode())

        assertEquals(deck.encode(), saved.encode())
        assertTrue(saved.isLearning("boon", en))
        assertFalse(saved.isLearning("boon", pl))
        assertTrue(saved.isLearning("puddle", en))
        assertTrue(saved.isLearning("puddle", pl))
    }

    @Test
    fun `legacy difficult-word recovery is copied then evolves independently`() {
        val legacy = DifficultyProgress.decode("boon=0;1;true")
        val migrated = DirectionalDifficulty.fromLegacy(legacy)
        val answered = migrated.recordAnswer("boon", en, true)

        assertFalse(answered.isDifficult("boon", en))
        assertTrue(answered.isDifficult("boon", pl))
        assertEquals(1, answered.of("boon", pl)?.recoveries)
        assertEquals(answered.encode(), DirectionalDifficulty.decode(answered.encode()).encode())
    }

    @Test
    fun `new difficulty and recovery streaks do not leak across directions`() {
        var difficulty = DirectionalDifficulty.NONE
        repeat(3) { difficulty = difficulty.recordAnswer("boon", pl, false) }
        difficulty = difficulty.recordAnswer("boon", en, true).recordAnswer("boon", en, true)

        assertTrue(difficulty.isDifficult("boon", pl))
        assertFalse(difficulty.isDifficult("boon", en))
        assertEquals(0, difficulty.of("boon", pl)?.recoveries)
    }

    @Test
    fun `invalid version-two data fails explicitly instead of dropping progress`() {
        listOf(
            "ENGLISH_FIRST=|",
            "ENGLISH_FIRST=|\nENGLISH_FIRST=|",
            "ENGLISH_FIRST=invalid|\nPOLISH_FIRST=|",
            "ENGLISH_FIRST=|boon\nPOLISH_FIRST=|",
            "ENGLISH_FIRST=boon=S1;2026-11-03;2026-11-02|\nPOLISH_FIRST=|",
        ).forEach { encoded ->
            assertThrows(encoded, IllegalArgumentException::class.java) { DirectionalProgress.decode(encoded) }
        }
    }
}
