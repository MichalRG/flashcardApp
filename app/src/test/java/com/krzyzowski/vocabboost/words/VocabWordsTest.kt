package com.krzyzowski.vocabboost.words

import com.krzyzowski.vocabboost.schedule.DeckProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VocabWordsTest {
    @Test
    fun `the bundled list has words to learn`() {
        assertTrue(VOCAB_WORDS.isNotEmpty())
    }

    @Test
    fun `every word has its own id so progress never mixes two words`() {
        val repeated = VOCAB_WORDS.groupBy { it.id }.filterValues { it.size > 1 }.keys

        assertEquals(emptySet<String>(), repeated)
    }

    @Test
    fun `every word id can be saved in the progress text and read back`() {
        val day = LocalDate.parse("2026-11-02")

        VOCAB_WORDS.forEach { word ->
            val saved = DeckProgress.NONE.recordAnswer(word.id, knew = true, day = day).encode()

            assertNotNull("progress of '${word.id}' did not survive saving", DeckProgress.decode(saved).of(word.id))
        }
    }

    @Test
    fun `every word has an English word, an example and a Polish translation`() {
        val withBlankField = VOCAB_WORDS.filter {
            it.id.isBlank() || it.english.isBlank() || it.example.isBlank() || it.polish.isBlank()
        }

        assertEquals(emptyList<VocabWord>(), withBlankField)
    }

    @Test
    fun `every example is one or two sentences`() {
        val sentenceEnd = Regex("(?<=[.!?])\\s+")

        val wrongLength = VOCAB_WORDS.filter { word ->
            word.example.trim().split(sentenceEnd).count { it.isNotBlank() } !in 1..2
        }.map { it.id }

        assertEquals(emptyList<String>(), wrongLength)
    }
}
