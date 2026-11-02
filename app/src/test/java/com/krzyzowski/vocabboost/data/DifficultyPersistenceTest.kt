package com.krzyzowski.vocabboost.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.schedule.DirectionalProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.PracticeDeck
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.time.FakeClock
import com.krzyzowski.vocabboost.words.VocabWordBuilder.words
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate

class DifficultyPersistenceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val clock = FakeClock(Instant.parse("2026-11-02T10:00:00Z"))
    private val dataStore by lazy { openStore(scope) }
    private val store by lazy { ProgressStore(dataStore, clock) }
    private val wordKey = stringPreferencesKey("word_progress_v1")
    private val difficultyKey = stringPreferencesKey("difficulty_progress_v1")

    private fun openStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    @After
    fun closeStore() {
        scope.cancel()
    }

    @Test
    fun `a fresh install has no difficult words`() = runTest {
        assertEquals(DirectionalDifficulty.NONE.encode(), store.difficulty.first().encode())
    }

    @Test
    fun `reading difficulty on upgrade leaves all existing progress settings and statistics unchanged`() = runTest {
        dataStore.edit {
            it[wordKey] = "boon=S1;2026-11-02;2026-10-30"
            it[intPreferencesKey("new_words_per_day")] = 5
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-01=300"
            it[stringPreferencesKey("streak_v1")] = Streak(4, LocalDate.parse("2026-11-01")).encode()
        }
        val before = dataStore.data.first().asMap()

        assertEquals(DirectionalDifficulty.NONE.encode(), store.difficulty.first().encode())
        assertFalse(store.difficulty.first().isDifficult("boon"))
        assertEquals(Stage.S1, store.deck.first().of("boon")?.stage)
        assertEquals(before, dataStore.data.first().asMap())

        store.recordAnswer("boon", knew = false)

        val after = dataStore.data.first()
        assertEquals(1, store.difficulty.first().of("boon")?.misses)
        before.filterKeys { it != wordKey }.forEach { (key, value) ->
            assertEquals("Preserved $key", value, after.asMap()[key])
        }
        assertEquals(LocalDate.parse("2026-10-30"), store.deck.first().of("boon")?.introducedOn)
        val screenTime = ScreenTimeLog(dataStore, clock)
        assertEquals(Streak(4, LocalDate.parse("2026-11-01")), screenTime.streak.first())
        assertEquals(300L, screenTime.dailySeconds.first().secondsOn(LocalDate.parse("2026-11-01")))
        assertEquals(5, SettingsStore(dataStore).newWordsPerDay.first())
    }

    @Test
    fun `a difficult word and its first successful recovery survive an app restart`() = runTest {
        val firstRun = Job()
        val before = ProgressStore(openStore(CoroutineScope(Dispatchers.IO + firstRun)), clock)
        repeat(3) { before.recordAnswer("boon", knew = false) }
        before.recordAnswer("boon", knew = true)
        firstRun.cancelAndJoin()

        val saved = store.difficulty.first()
        assertTrue(saved.isDifficult("boon"))
        assertEquals(1, saved.of("boon")?.recoveries)
        assertEquals(Stage.S1, store.deck.first().of("boon")?.stage)

        store.recordAnswer("boon", knew = true)

        assertFalse(store.difficulty.first().isDifficult("boon"))
        assertNull(store.difficulty.first().of("boon"))
        assertEquals(Stage.S3, store.deck.first().of("boon")?.stage)
    }

    @Test
    fun `one transaction saves both the ordinary schedule and difficulty`() = runTest {
        repeat(3) {
            store.recordAnswer("boon", knew = false)
            clock.instant = clock.instant.plusSeconds(86_400)
        }

        val saved = dataStore.data.first()
        val deck = DirectionalProgress.decode(requireNotNull(saved[stringPreferencesKey("directional_word_progress_v2")]))
        val difficulty = DirectionalDifficulty.decode(requireNotNull(saved[stringPreferencesKey("directional_difficulty_v2")]))
        assertEquals(Stage.S1, deck.of("boon")?.stage)
        assertTrue(deck.isLearning("boon", CardDirection.ENGLISH_FIRST))
        assertEquals(LocalDate.parse("2026-11-02"), deck.of("boon")?.introducedOn)
        assertTrue(difficulty.isDifficult("boon"))
        assertEquals(3, difficulty.of("boon")?.misses)
        store.recordAnswer("boon", knew = true)
        assertEquals(LocalDate.parse("2026-11-06"), store.deck.first().of("boon")?.due)
    }

    @Test
    fun `concurrent answers cannot lose miss streak updates`() = runTest {
        coroutineScope {
            (1..3).map { async { store.recordAnswer("boon", knew = false) } }.awaitAll()
        }

        assertTrue(store.difficulty.first().isDifficult("boon"))
        assertEquals(3, store.difficulty.first().of("boon")?.misses)
        assertEquals(Stage.S1, store.deck.first().of("boon")?.stage)
    }

    @Test
    fun `free revision and language selection change neither schedule nor difficulty streaks`() = runTest {
        repeat(3) { store.recordAnswer("boon", knew = false) }
        val beforeDeck = store.deck.first()
        val beforeDifficulty = store.difficulty.first()
        val settings = SettingsStore(dataStore)
        val practice = PracticeDeck.difficult(
            words("boon", "puddle"), beforeDeck.forDirection(CardDirection.ENGLISH_FIRST).schedule,
            beforeDifficulty.forDirection(CardDirection.ENGLISH_FIRST),
        )

        CardDirection.entries.forEach { direction ->
            settings.setPracticeDirection(direction)
            direction.front(practice.wordAt(0))
        }
        TestMode.entries.forEach { settings.setTestMode(it) }

        assertEquals(beforeDeck.encode(), store.deck.first().encode())
        assertEquals(beforeDifficulty.encode(), store.difficulty.first().encode())
        assertTrue(store.difficulty.first().isDifficult("boon"))
        assertEquals(0, store.difficulty.first().of("boon")?.recoveries)
    }

    @Test
    fun `an invalid difficulty entry cannot cause a partial schedule update`() = runTest {
        dataStore.edit {
            it[wordKey] = "boon=S3;2026-11-02;2026-10-30"
            it[difficultyKey] = "invalid"
        }
        val before = dataStore.data.first().asMap()

        try {
            store.recordAnswer("boon", knew = true)
            fail("Invalid difficulty data must reject the entire answer transaction")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Invalid difficulty progress entry", expected.message)
        }

        assertEquals(before, dataStore.data.first().asMap())
    }
}
