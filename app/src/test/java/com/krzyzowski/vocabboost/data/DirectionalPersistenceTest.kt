package com.krzyzowski.vocabboost.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DirectionalWorkload
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.session.CardDirection
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate

class DirectionalPersistenceTest {
    @get:Rule
    val folder = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val clock = FakeClock(Instant.parse("2026-11-02T10:00:00Z"))
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST
    private val today = LocalDate.parse("2026-11-02")
    private val dataStore by lazy { openStore(scope) }
    private val store by lazy { ProgressStore(dataStore, clock) }

    private fun openStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    @After
    fun closeStore() {
        scope.cancel()
    }

    @Test
    fun `migration copies old records in memory and preserves legacy backups settings and statistics`() = runTest {
        val oldWord = "boon=S7;2026-11-02;2026-10-20,puddle=RET;-;2026-03-01"
        val oldDifficulty = "boon=0;1;true"
        dataStore.edit {
            it[stringPreferencesKey("word_progress_v1")] = oldWord
            it[stringPreferencesKey("difficulty_progress_v1")] = oldDifficulty
            it[intPreferencesKey("new_words_per_day")] = 5
            it[intPreferencesKey("daily_goal_minutes")] = 5
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-01=300"
            it[stringPreferencesKey("streak_v1")] = "4;2026-11-01"
            it[stringPreferencesKey("test_mode")] = TestMode.POLISH_FIRST.name
        }
        val settings = SettingsStore(dataStore)
        settings.setPracticeDirection(pl)
        settings.markNotificationsAsked()
        val before = dataStore.data.first().asMap()

        CardDirection.entries.forEach { direction ->
            assertEquals(oldWord, store.deck.first().forDirection(direction).schedule.encode())
            assertEquals(oldDifficulty, store.difficulty.first().forDirection(direction).encode())
        }
        assertEquals(before, dataStore.data.first().asMap())

        store.recordAnswer("boon", pl, knew = false)

        val after = dataStore.data.first().asMap()
        before.forEach { (key, value) -> assertEquals("Existing $key was modified", value, after[key]) }
        assertEquals(Stage.S1, store.deck.first().of("boon", pl)?.stage)
        assertEquals(Stage.S7, store.deck.first().of("boon", en)?.stage)
        assertEquals(1, store.difficulty.first().of("boon", en)?.recoveries)
        assertEquals(0, store.difficulty.first().of("boon", pl)?.recoveries)
        assertEquals(Stage.MAINTENANCE, store.deck.first().of("puddle", en)?.stage)
        assertEquals(Stage.MAINTENANCE, store.deck.first().of("puddle", pl)?.stage)
        assertEquals(5, settings.newWordsPerDay.first())
        assertEquals(5, settings.dailyGoalMinutes.first())
        assertEquals(TestMode.POLISH_FIRST, settings.testMode.first())
        assertEquals(pl, settings.practiceDirection.first())
        assertTrue(settings.notificationsAsked.first())
        val time = ScreenTimeLog(dataStore, clock)
        assertEquals(300L, time.dailySeconds.first().secondsOn(today.minusDays(1)))
        assertEquals(Streak(4, today.minusDays(1)), time.streak.first())
    }

    @Test
    fun `unfinished learning survives closing and reopening and completes at the one-day interval`() = runTest {
        val firstRun = Job()
        val before = ProgressStore(openStore(CoroutineScope(Dispatchers.IO + firstRun)), clock)
        repeat(2) { before.recordAnswer("boon", pl, false) }
        before.recordAnswer("boon", en, true)
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        assertTrue(saved.isLearning("boon", pl))
        assertFalse(saved.isLearning("boon", en))
        assertEquals(1, saved.introducedOn(today))
        assertEquals(2, store.difficulty.first().of("boon", pl)?.misses)
        assertEquals("", store.difficulty.first().forDirection(en).encode())
        val workload = DirectionalWorkload.of(words("boon"), saved, today, 0, TestMode.MIXED)
        assertEquals(pl, workload.cards.single().direction)

        clock.instant = clock.instant.plusSeconds(172_800)
        store.recordAnswer("boon", pl, true)
        val completed = store.deck.first()

        assertFalse(completed.isLearning("boon", pl))
        assertEquals(Stage.S1, completed.of("boon", pl)?.stage)
        assertEquals(today.plusDays(3), completed.of("boon", pl)?.due)
        assertEquals(today, completed.of("boon", pl)?.introducedOn)
        assertEquals(today.plusDays(1), completed.of("boon", en)?.due)
    }

    @Test
    fun `concurrent first answers cannot consume two slots or overwrite the opposite direction`() = runTest {
        coroutineScope {
            listOf(en, pl).map { direction -> async { store.recordAnswer("boon", direction, true) } }.awaitAll()
        }

        val saved = store.deck.first()
        assertEquals(1, saved.introducedOn(today))
        CardDirection.entries.forEach { direction ->
            assertFalse(saved.isLearning("boon", direction))
            assertEquals(Stage.S1, saved.of("boon", direction)?.stage)
            assertEquals(today.plusDays(1), saved.of("boon", direction)?.due)
        }
    }

    @Test
    fun `initial retries do not promote the spaced-repetition stage or clear difficulty in the other direction`() = runTest {
        repeat(3) { store.recordAnswer("boon", pl, false) }
        store.recordAnswer("boon", pl, true)

        assertEquals(1, store.deck.first().introducedOn(today))
        assertEquals(Stage.S1, store.deck.first().of("boon", pl)?.stage)
        assertTrue(store.difficulty.first().isDifficult("boon", pl))
        assertFalse(store.difficulty.first().isDifficult("boon", en))
        assertEquals(1, store.difficulty.first().of("boon", pl)?.recoveries)

        clock.instant = clock.instant.plusSeconds(86_400)
        store.recordAnswer("boon", pl, true)
        assertEquals(Stage.S3, store.deck.first().of("boon", pl)?.stage)
        assertFalse(store.difficulty.first().isDifficult("boon", pl))
        assertTrue(store.deck.first().isLearning("boon", en))
    }

    @Test
    fun `invalid version-two progress never silently falls back to the older shared record`() = runTest {
        dataStore.edit {
            it[stringPreferencesKey("word_progress_v1")] = "boon=S7;2026-11-02;2026-10-20"
            it[stringPreferencesKey("directional_word_progress_v2")] = "invalid"
        }
        val before = dataStore.data.first().asMap()
        try {
            store.recordAnswer("boon", en, true)
            fail("Corrupt version-two progress must not be overwritten using the legacy backup")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Invalid directional progress entry", expected.message)
        }
        assertEquals(before, dataStore.data.first().asMap())
    }

    @Test
    fun `invalid version-two difficulty prevents a partial schedule write`() = runTest {
        store.recordAnswer("boon", en, true)
        dataStore.edit {
            it[stringPreferencesKey("directional_difficulty_v2")] = "invalid"
        }
        val before = dataStore.data.first().asMap()
        try {
            store.recordAnswer("boon", pl, true)
            fail("A corrupt difficulty record must not cause a partial schedule update")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Invalid directional progress entry", expected.message)
        }
        assertEquals(before, dataStore.data.first().asMap())
    }
}
