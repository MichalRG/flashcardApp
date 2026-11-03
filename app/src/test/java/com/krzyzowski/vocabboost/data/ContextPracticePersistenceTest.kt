package com.krzyzowski.vocabboost.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.ContextExercise
import com.krzyzowski.vocabboost.session.PracticeDeck
import com.krzyzowski.vocabboost.session.PracticeMode
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.time.FakeClock
import com.krzyzowski.vocabboost.words.VOCAB_WORDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class ContextPracticePersistenceTest {
    @get:Rule
    val folder = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val clock = FakeClock(Instant.parse("2026-11-03T10:00:00Z"))
    private val dataStore by lazy { openStore(scope) }
    private val settings by lazy { SettingsStore(dataStore) }
    private val progress by lazy { ProgressStore(dataStore, clock) }

    private fun openStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    @After
    fun closeStore() {
        scope.cancel()
    }

    @Test
    fun `an existing installation defaults to Flashcards without rewriting its preferences`() = runTest {
        dataStore.edit {
            it[stringPreferencesKey("word_progress_v1")] = "dump=S7;2026-11-10;2026-10-20"
            it[stringPreferencesKey("difficulty_progress_v1")] = "dump=0;1;true"
            it[intPreferencesKey("new_words_per_day")] = 3
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-02=300"
            it[stringPreferencesKey("streak_v1")] = "4;2026-11-02"
        }
        val before = dataStore.data.first().asMap()

        assertEquals(PracticeMode.FLASHCARDS, settings.practiceMode.first())
        assertEquals(before, dataStore.data.first().asMap())
        assertEquals(3, settings.newWordsPerDay.first())
    }

    @Test
    fun `Context selection and ungraded exercise browsing preserve all directional progress and other preferences`() = runTest {
        repeat(3) { progress.recordAnswer("dump", CardDirection.POLISH_FIRST, false) }
        progress.recordAnswer("dump", CardDirection.ENGLISH_FIRST, true)
        settings.setPracticeDirection(CardDirection.POLISH_FIRST)
        settings.setTestMode(TestMode.ENGLISH_FIRST)
        settings.setNewWordsPerDay(3)
        settings.setDailyGoalMinutes(5)
        dataStore.edit {
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-02=300"
            it[stringPreferencesKey("streak_v1")] = "4;2026-11-02"
        }
        val before = dataStore.data.first().asMap()
        val deckBefore = progress.deck.first()
        val difficultyBefore = progress.difficulty.first().encode()
        val selected = deckBefore.forDirection(CardDirection.POLISH_FIRST)

        PracticeMode.entries.forEach { mode ->
            settings.setPracticeMode(mode)
            assertEquals(mode, settings.practiceMode.first())
            val deck = PracticeDeck(
                VOCAB_WORDS, selected.schedule, CardDirection.POLISH_FIRST, selected.learning, mode,
            )
            (0 until deck.size).forEach { position ->
                if (mode == PracticeMode.CONTEXT) {
                    val exercise = ContextExercise(deck.wordAt(position))
                    exercise.prompt.replace(ContextExercise.GAP, exercise.answer)
                }
            }
        }

        val after = dataStore.data.first().asMap().filterKeys { it != stringPreferencesKey("practice_mode") }
        assertEquals(before, after)
        assertEquals(deckBefore.encode(), progress.deck.first().encode())
        assertEquals(difficultyBefore, progress.difficulty.first().encode())
        assertTrue(progress.deck.first().isLearning("dump", CardDirection.POLISH_FIRST))
        assertTrue(progress.difficulty.first().isDifficult("dump", CardDirection.POLISH_FIRST))
        assertFalse(progress.deck.first().isLearning("dump", CardDirection.ENGLISH_FIRST))
        assertEquals(CardDirection.POLISH_FIRST, settings.practiceDirection.first())
        assertEquals(TestMode.ENGLISH_FIRST, settings.testMode.first())
    }

    @Test
    fun `Context mode survives restart independently of Practice language and test mode`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        val before = SettingsStore(original)
        before.setPracticeMode(PracticeMode.CONTEXT)
        before.setPracticeDirection(CardDirection.POLISH_FIRST)
        before.setTestMode(TestMode.ENGLISH_FIRST)
        firstRun.cancelAndJoin()

        assertEquals(PracticeMode.CONTEXT, settings.practiceMode.first())
        assertEquals(CardDirection.POLISH_FIRST, settings.practiceDirection.first())
        assertEquals(TestMode.ENGLISH_FIRST, settings.testMode.first())
        val directional = progress.deck.first().forDirection(CardDirection.POLISH_FIRST)
        val difficult = PracticeDeck.difficult(VOCAB_WORDS, directional.schedule, DifficultyProgress.NONE)
        assertEquals(PracticeMode.FLASHCARDS, difficult.mode)
    }

    @Test
    fun `invalid saved Practice modes fail explicitly rather than silently selecting Flashcards`() = runTest {
        dataStore.edit { it[stringPreferencesKey("practice_mode")] = "UNKNOWN" }
        val before = dataStore.data.first().asMap()

        try {
            settings.practiceMode.first()
            fail("Invalid saved Practice mode must not silently select Flashcards")
        } catch (expected: IllegalArgumentException) {
            assertTrue(requireNotNull(expected.message).contains("UNKNOWN"))
        }
        assertEquals(before, dataStore.data.first().asMap())
    }
}
