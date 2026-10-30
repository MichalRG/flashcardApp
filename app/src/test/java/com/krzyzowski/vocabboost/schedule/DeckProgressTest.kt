package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.schedule.DeckBuilder.reaching
import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DeckProgressTest {
    private val golden = "boon=S3;2026-11-02;2026-10-30,puddle=RET;-;2026-03-01"

    @Test
    fun `saved progress reads back from the version 1 text`() {
        val deck = DeckProgress.decode(golden)

        assertEquals(
            WordProgress(Stage.S3, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-10-30")),
            deck.of("boon"),
        )
        assertEquals(WordProgress(Stage.RETIRED, null, LocalDate.parse("2026-03-01")), deck.of("puddle"))
    }

    @Test
    fun `progress is saved as the version 1 text sorted by word id`() {
        val deck = DeckProgress.NONE
            .reaching("puddle", Stage.RETIRED, LocalDate.parse("2026-03-01"))
            .reaching("boon", Stage.S3, LocalDate.parse("2026-10-30"))

        assertEquals(golden, deck.encode())
    }

    @Test
    fun `a malformed entry loses only that word`() {
        val text = listOf(
            "boon=S3;2026-11-02;2026-10-30",
            "no-equals-sign",
            "unknown-stage=S99;2026-11-02;2026-10-30",
            "bad-due-date=S3;2026-13-45;2026-10-30",
            "bad-intro-date=S3;2026-11-02;yesterday",
            "missing-field=S3;2026-11-02",
            "puddle=RET;-;2026-03-01",
        ).joinToString(",")

        val deck = DeckProgress.decode(text)

        assertEquals(golden, deck.encode())
    }

    @Test
    fun `no saved text means no word has progress`() {
        val deck = DeckProgress.decode("")

        assertNull(deck.of("boon"))
        assertEquals("", deck.encode())
    }

    @Test
    fun `the first answer of a new word, whichever button, puts it on the 1-day step due tomorrow`() {
        val day = LocalDate.parse("2026-11-02")

        listOf(true, false).forEach { knew ->
            val progress = DeckProgress.NONE.recordAnswer("boon", knew, day).of("boon")

            assertEquals(
                "first answer knew=$knew",
                WordProgress(Stage.S1, LocalDate.parse("2026-11-03"), introducedOn = day),
                progress,
            )
        }
    }

    @Test
    fun `answering one word leaves the other words untouched`() {
        val before = DeckProgress.NONE.reaching("boon", Stage.S7, LocalDate.parse("2026-10-30"))

        val after = before.recordAnswer("puddle", knew = true, day = LocalDate.parse("2026-11-02"))

        assertEquals(before.of("boon"), after.of("boon"))
    }

    @Test
    fun `progress follows the word id through inserted, reordered, corrected, removed and re-added words`() {
        val today = LocalDate.parse("2026-11-02")
        val answered = DeckProgress.NONE.reaching("boon", Stage.S3, LocalDate.parse("2026-10-30"))
        val saved = DeckProgress.decode(answered.encode())
        val boonCorrected = word("boon", english = "boon (noun)")

        val insertedReorderedCorrected = listOf(word("fat-chance"), word("puddle"), word("reluctant"), boonCorrected)
        val withEdits = Workload.of(insertedReorderedCorrected, saved, today, newWordsPerDay = 5)
        assertEquals(listOf(boonCorrected), withEdits.reviews)
        assertEquals(listOf("fat-chance", "puddle", "reluctant"), withEdits.newWords.map { it.id })

        val removed = words("fat-chance", "puddle", "reluctant")
        assertEquals(emptyList<String>(), Workload.of(removed, saved, today, newWordsPerDay = 0).reviews.map { it.id })

        val savedWhileRemoved = DeckProgress.decode(saved.recordAnswer("puddle", knew = true, day = today).encode())
        assertNotNull(savedWhileRemoved.of("boon"))
        val readded = Workload.of(insertedReorderedCorrected, savedWhileRemoved, today, newWordsPerDay = 0)
        assertEquals(listOf("boon"), readded.reviews.map { it.id })
    }
}
