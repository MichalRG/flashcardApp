package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VOCAB_WORDS
import com.krzyzowski.vocabboost.words.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextExerciseTest {
    @Test
    fun `every bundled word has exactly one gap and restores its original context sentence`() {
        assertEquals(144, VOCAB_WORDS.size)
        VOCAB_WORDS.forEach { word ->
            val exercise = ContextExercise(word)
            assertEquals("Gap count for ${word.id}", 2, exercise.prompt.split(ContextExercise.GAP).size)
            assertFalse("Answer leaks for ${word.id}", exercise.prompt.contains(exercise.answer, ignoreCase = true))
            assertEquals(word.contextSentence, exercise.completedSentence)
            assertEquals(word.contextSentence, exercise.prompt.replace(ContextExercise.GAP, exercise.answer))
        }
    }

    @Test
    fun `wear and tear uses the requested car sentence`() {
        val exercise = ContextExercise(VOCAB_WORDS.single { it.id == "wear-and-tear" })

        assertEquals("The car is showing signs of ______.", exercise.prompt)
        assertEquals("wear and tear", exercise.answer)
        assertEquals("The car is showing signs of wear and tear.", exercise.completedSentence)
    }

    @Test
    fun `inflected verbs pronouns and alternative display forms reveal the grammatical expression`() {
        val answers = mapOf(
            "overtake" to "overtaking",
            "unscrew" to "unscrewed",
            "graduate-from-university" to "graduated from university",
            "bring-traffic-to-a-halt" to "brought traffic to a halt",
            "give-someone-a-lift" to "gave me a lift",
            "blow-out-tyre" to "blew out",
            "windscreen-wiper" to "windscreen wipers",
            "clutch" to "clutch",
            "clutch-car" to "clutch",
            "run-a-blood-test" to "run a blood test",
        )
        answers.forEach { (id, answer) ->
            assertEquals(id, answer, ContextExercise(VOCAB_WORDS.single { it.id == id }).answer)
        }
    }

    @Test
    fun `sentences that repeat an expression use one context sentence without leaking a second answer`() {
        listOf("spare-room", "parking-ticket").forEach { id ->
            val word = VOCAB_WORDS.single { it.id == id }
            val exercise = ContextExercise(word)
            assertTrue(word.example.length > exercise.completedSentence.length)
            assertFalse(exercise.prompt.contains(exercise.answer))
            assertEquals(word.example.substringBefore(".") + ".", exercise.completedSentence)
        }
    }

    @Test
    fun `answer capitalization is preserved rather than forcing the dictionary spelling into the sentence`() {
        val exercise = ContextExercise(VOCAB_WORDS.single { it.id == "dump-out" })

        assertEquals("Dump out", exercise.answer)
        assertEquals("______ the dirty water before you refill the bucket.", exercise.prompt)
    }

    @Test
    fun `context answers are literal expressions rather than regular expressions`() {
        val exercise = ContextExercise(VocabWord("cpp", "C++", "She writes programs in C++ every day.", "C++"))

        assertEquals("C++", exercise.answer)
        assertEquals("She writes programs in ______ every day.", exercise.prompt)
    }

    @Test
    fun `a substring inside another word is not a valid context answer`() {
        val word = VocabWord("art", "art", "The party starts at noon.", "sztuka")

        val error = assertThrows(IllegalArgumentException::class.java) { ContextExercise(word) }
        assertTrue(requireNotNull(error.message).contains("art"))
    }

    @Test
    fun `missing repeated or empty answers and pre-existing gaps fail explicitly`() {
        val word = VocabWord("cat", "cat", "The cat is asleep.", "kot")
        listOf(
            word.copy(contextAnswer = ""),
            word.copy(contextAnswer = " cat "),
            word.copy(contextSentence = "It is asleep."),
            word.copy(contextSentence = "The cat watched another cat."),
            word.copy(contextSentence = "The cat is ______."),
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { ContextExercise(invalid) }
        }
    }

    @Test
    fun `building and revealing a context exercise never changes the original vocabulary card`() {
        val word = VOCAB_WORDS.single { it.id == "overtake" }
        val before = word.copy()
        val exercise = ContextExercise(word)
        exercise.prompt.replace(ContextExercise.GAP, exercise.answer)

        assertEquals(before, word)
        assertEquals("overtake", word.english)
        assertEquals("I waited for a clear stretch of road before overtaking the lorry.", word.example)
    }
}
