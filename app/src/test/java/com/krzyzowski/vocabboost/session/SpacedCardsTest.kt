package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedCardsTest {
    private fun card(id: String, direction: CardDirection = CardDirection.POLISH_FIRST, new: Boolean = false) =
        TestCard(word(id), new, direction)

    private fun pairs(count: Int, new: Boolean = false) = (1..count).flatMap { index ->
        CardDirection.entries.map { direction -> card("word-$index", direction, new) }
    }

    private fun assertGap(cards: List<TestCard>) {
        cards.windowed(4, partialWindows = true).forEach { window ->
            assertEquals("Repeated word inside ${window.map { it.word.id }}", window.size, window.map { it.word.id }.distinct().size)
        }
    }

    /** Every pick respects the 3-card gap, or (when none can) is the least recently shown word. */
    private fun assertBestEffort(planned: List<TestCard>, shown: List<TestCard> = emptyList()) {
        val history = shown.map { it.word.id }
        val order = planned.map { it.word.id }
        order.indices.forEach { index ->
            val violation = bestEffortViolation(history + order.take(index), order.drop(index))
            assertEquals("At position $index of $order", null, violation)
        }
    }

    @Test
    fun `paired mixed reviews are all retained without adjacent opposite directions`() {
        (4..40).forEach { count ->
            val original = pairs(count)
            val planned = SpacedCards.of(original)

            assertEquals(count * 2, planned.ready.size)
            assertTrue(planned.deferred.isEmpty())
            assertEquals(original.toSet(), planned.ready.toSet())
            assertGap(planned.ready)
        }
    }

    @Test
    fun `new direction pairs use the same gap rule without changing their metadata`() {
        val original = pairs(8, new = true)
        val planned = SpacedCards.of(original)

        assertEquals(16, planned.ready.size)
        assertTrue(planned.ready.all { it.isNew && it.isLearning })
        assertEquals(original.toSet(), planned.ready.toSet())
        assertGap(planned.ready)
    }

    @Test
    fun `one two or three distinct words place their opposite directions with the largest gap available`() {
        (1..3).forEach { count ->
            val original = pairs(count)
            val planned = SpacedCards.of(original)

            assertEquals(count * 2, planned.ready.size)
            assertTrue(planned.deferred.isEmpty())
            assertEquals(original.toSet(), planned.ready.toSet())
            (1..count).forEach { index ->
                val positions = planned.ready.indices.filter { planned.ready[it].word.id == "word-$index" }
                assertEquals(count - 1, positions.last() - positions.first() - 1)
            }
            assertBestEffort(planned.ready)
        }
    }

    @Test
    fun `when no card meets the minimum gap the word shown least recently comes next`() {
        val shown = listOf(card("a"), card("b"), card("c"))
        val pending = listOf(card("c"), card("a", CardDirection.ENGLISH_FIRST, new = true), card("a"), card("b"))
        val planned = SpacedCards.of(pending, shown)

        assertTrue(planned.deferred.isEmpty())
        assertEquals(
            listOf(card("a"), card("b"), card("c"), card("a", CardDirection.ENGLISH_FIRST, new = true)),
            planned.ready,
        )
    }

    @Test
    fun `the fallback keeps supplied order between prompts of the least recently shown word`() {
        val shown = listOf(card("a"), card("b"), card("c"))
        val first = card("a", CardDirection.ENGLISH_FIRST)
        val second = card("a", CardDirection.POLISH_FIRST)

        assertEquals(listOf(first, second), SpacedCards.of(listOf(first, second), shown).ready)
        assertEquals(listOf(second, first), SpacedCards.of(listOf(second, first), shown).ready)
    }

    @Test
    fun `four words allow three spacers while five words prefer four`() {
        listOf(4, 5).forEach { count ->
            val cards = SpacedCards.of(pairs(count)).ready
            (1..count).forEach { index ->
                val positions = cards.indices.filter { cards[it].word.id == "word-$index" }
                assertEquals(count - 1, positions.last() - positions.first() - 1)
            }
        }
    }

    @Test
    fun `a single repeated word fits when three other words are available`() {
        val original = listOf(card("a"), card("a", CardDirection.ENGLISH_FIRST), card("b"), card("c"), card("d"))
        val planned = SpacedCards.of(original)

        assertEquals(listOf("a", "b", "c", "d", "a"), planned.ready.map { it.word.id })
        assertTrue(planned.deferred.isEmpty())
        assertGap(planned.ready)
    }

    @Test
    fun `replanning considers cards already shown instead of resetting the spacing history`() {
        val shown = listOf(card("a"))
        val pending = listOf(card("a", CardDirection.ENGLISH_FIRST), card("b"), card("c"), card("d"), card("e"))
        val planned = SpacedCards.of(pending, shown)

        assertEquals(listOf("b", "c", "d", "e", "a"), planned.ready.map { it.word.id })
        assertGap(shown + planned.ready)
    }

    @Test
    fun `new words fill a gap only while existing reviews cannot be shown safely`() {
        val original = listOf(
            card("a"), card("a", CardDirection.ENGLISH_FIRST),
            card("b", new = true), card("c", new = true), card("d", new = true), card("e", new = true),
        )
        val planned = SpacedCards.of(original)

        assertEquals(listOf("a", "b", "c", "d", "a", "e"), planned.ready.map { it.word.id })
        assertTrue(planned.deferred.isEmpty())
        assertGap(planned.ready)
    }

    @Test
    fun `unique reviews and unfinished directions retain their supplied priority`() {
        val original = listOf(card("older"), card("unfinished").copy(isLearning = true), card("younger"))

        assertEquals(original, SpacedCards.of(original).ready)
    }

    @Test
    fun `uneven repeat counts do not exhaust all spacers before the most frequent word`() {
        val original = listOf("a", "a", "a", "b", "b", "c", "c", "d", "d", "e").map { card(it) }
        val planned = SpacedCards.of(original)

        assertEquals(original.size, planned.ready.size)
        assertTrue(planned.deferred.isEmpty())
        assertGap(planned.ready)
    }

    @Test
    fun `three-card gaps are used when four would unnecessarily strand a third prompt for the same word`() {
        val original = listOf("a", "a", "a", "b", "c", "d", "e", "f", "g").map { card(it) }
        val planned = SpacedCards.of(original)

        assertEquals(original.size, planned.ready.size)
        assertTrue(planned.deferred.isEmpty())
        assertEquals(listOf(0, 4, 8), planned.ready.indices.filter { planned.ready[it].word.id == "a" })
        assertGap(planned.ready)
    }

    @Test
    fun `planning is immutable preserves every candidate and handles empty decks`() {
        assertEquals(SpacedCards(emptyList(), emptyList()), SpacedCards.of(emptyList()))
        (1..8).forEach { count ->
            val original = (1..count).flatMap { index -> List(1 + index % 3) { card("word-$index") } }
            val before = original.toList()
            val planned = SpacedCards.of(original)

            assertEquals(before, original)
            assertTrue(planned.deferred.isEmpty())
            assertEquals(original.groupingBy { it }.eachCount(), planned.ready.groupingBy { it }.eachCount())
            assertBestEffort(planned.ready)
        }
    }
}
