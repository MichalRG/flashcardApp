package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WorkloadTest {
    private val today = LocalDate.parse("2026-11-02")

    @Test
    fun `a test lists due words most overdue first, equally overdue ones in list order, then new words`() {
        val list = words("pearl", "anchor", "kettle", "lantern", "orchard", "meadow", "harbor")
        val deck = DeckProgress.NONE
            .reaching("pearl", Stage.S3, LocalDate.parse("2026-10-25"))
            .reaching("anchor", Stage.S3, LocalDate.parse("2026-10-25"))
            .reaching("lantern", Stage.S3, LocalDate.parse("2026-10-20"))
            .reaching("orchard", Stage.S7, LocalDate.parse("2026-10-26"))
            .reaching("meadow", Stage.S3, LocalDate.parse("2026-10-31"))

        val workload = Workload.of(list, deck, today, newWordsPerDay = 1)

        // lantern is due 10-23, pearl and anchor 10-28, orchard today, meadow only tomorrow
        assertEquals(listOf("lantern", "pearl", "anchor", "orchard"), workload.reviews.map { it.id })
        assertEquals(listOf("kettle"), workload.newWords.map { it.id })
        assertEquals(5, workload.waiting)
    }

    @Test
    fun `new words come in list order and only the ones not answered yet are offered`() {
        val list = words("puddle", "boon", "thorough", "bargain", "reluctant", "grant", "fat")
        val deck = DeckProgress.NONE.recordAnswer("boon", knew = true, day = LocalDate.parse("2026-11-01"))

        val workload = Workload.of(list, deck, today, newWordsPerDay = 3)

        assertEquals(listOf("puddle", "thorough", "bargain"), workload.newWords.map { it.id })
    }

    @Test
    fun `new words answered today use up today's quota`() {
        val list = words("puddle", "boon", "thorough", "bargain", "reluctant", "grant", "fat", "cope")
        val deck = DeckProgress.NONE
            .recordAnswer("puddle", knew = true, day = today)
            .recordAnswer("boon", knew = false, day = today)
            .recordAnswer("thorough", knew = true, day = today)

        val workload = Workload.of(list, deck, today, newWordsPerDay = 5)

        assertEquals(emptyList<String>(), workload.reviews.map { it.id })
        assertEquals(listOf("bargain", "reluctant"), workload.newWords.map { it.id })
    }

    @Test
    fun `the quota starts again the next day and yesterday's words come back as reviews`() {
        val list = words("puddle", "boon", "thorough", "bargain", "reluctant", "grant", "fat", "cope")
        val deck = DeckProgress.NONE
            .recordAnswer("puddle", knew = true, day = today)
            .recordAnswer("boon", knew = false, day = today)
            .recordAnswer("thorough", knew = true, day = today)

        val tomorrow = Workload.of(list, deck, today.plusDays(1), newWordsPerDay = 5)

        assertEquals(listOf("puddle", "boon", "thorough"), tomorrow.reviews.map { it.id })
        assertEquals(listOf("bargain", "reluctant", "grant", "fat", "cope"), tomorrow.newWords.map { it.id })
    }

    @Test
    fun `lowering the daily limit mid-day stops more new words today but takes none back`() {
        val list = words("puddle", "boon", "thorough", "bargain", "reluctant")
        val deck = DeckProgress.NONE
            .recordAnswer("puddle", knew = true, day = today)
            .recordAnswer("boon", knew = true, day = today)
            .recordAnswer("thorough", knew = true, day = today)

        assertEquals(emptyList<String>(), Workload.of(list, deck, today, newWordsPerDay = 3).newWords.map { it.id })
        assertEquals(emptyList<String>(), Workload.of(list, deck, today, newWordsPerDay = 2).newWords.map { it.id })
        assertEquals(3, deck.introducedOn(today))
    }

    @Test
    fun `a limit of zero new words a day offers only reviews`() {
        val list = words("puddle", "boon", "thorough")
        val deck = DeckProgress.NONE.reaching("boon", Stage.S3, LocalDate.parse("2026-10-20"))

        val workload = Workload.of(list, deck, today, newWordsPerDay = 0)

        assertEquals(listOf("boon"), workload.reviews.map { it.id })
        assertEquals(emptyList<String>(), workload.newWords.map { it.id })
    }

    @Test
    fun `answering an old word again does not use up today's new-word quota`() {
        val list = words("puddle", "boon", "thorough")
        val deck = DeckProgress.NONE
            .reaching("puddle", Stage.S3, LocalDate.parse("2026-10-20"))
            .recordAnswer("puddle", knew = false, day = today)

        val workload = Workload.of(list, deck, today, newWordsPerDay = 2)

        assertEquals(listOf("boon", "thorough"), workload.newWords.map { it.id })
    }

    @Test
    fun `thirty due reviews remain available while even a legacy larger limit adds at most eight new words`() {
        val reviewIds = (1..30).map { "review-$it" }
        val newIds = (1..10).map { "new-$it" }
        val list = words(*(reviewIds + newIds).toTypedArray())
        val deck = reviewIds.fold(DeckProgress.NONE) { progress, id ->
            progress.reaching(id, Stage.S3, today.minusDays(10))
        }

        val workload = Workload.of(list, deck, today, newWordsPerDay = 20)

        assertEquals(reviewIds, workload.reviews.map { it.id })
        assertEquals(newIds.take(8), workload.newWords.map { it.id })
        assertEquals(38, workload.waiting)
    }

    @Test
    fun `separate tests share the same eight-new-word allowance`() {
        val list = words(*(1..10).map { "word-$it" }.toTypedArray())
        val deck = list.take(8).fold(DeckProgress.NONE) { progress, word ->
            progress.recordAnswer(word.id, knew = false, day = today)
        }

        val remaining = Workload.of(list, deck, today, newWordsPerDay = 8)

        assertEquals(emptyList<String>(), remaining.newWords.map { it.id })
        assertEquals(0, remaining.waiting)
        assertEquals(8, Workload.of(list, deck, today.plusDays(1), newWordsPerDay = 8).reviews.size)
    }

    @Test
    fun `a retired word never returns to a test, however many years pass`() {
        val list = words("puddle", "boon")
        val deck = DeckProgress.NONE.reaching("puddle", Stage.RETIRED, LocalDate.parse("2026-10-01"))

        val workload = Workload.of(list, deck, LocalDate.parse("2126-01-01"), newWordsPerDay = 5)

        assertEquals(emptyList<String>(), workload.reviews.map { it.id })
        assertEquals(listOf("boon"), workload.newWords.map { it.id })
    }
}
