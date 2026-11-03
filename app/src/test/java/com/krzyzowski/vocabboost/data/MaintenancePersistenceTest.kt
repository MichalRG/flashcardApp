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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate

class MaintenancePersistenceTest {
    @get:Rule
    val folder = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val clock = FakeClock(Instant.parse("2026-11-03T10:00:00Z"))
    private val today = LocalDate.parse("2026-11-03")
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST
    private val dataStore by lazy { openStore(scope) }
    private val store by lazy { ProgressStore(dataStore, clock) }

    private fun openStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    @After
    fun closeStore() {
        scope.cancel()
    }

    @Test
    fun `legacy shared retirement returns immediately and its annual date survives restart without changing settings or statistics`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        original.edit {
            it[stringPreferencesKey("word_progress_v1")] = "boon=RET;-;2026-03-01"
            it[stringPreferencesKey("difficulty_progress_v1")] = "boon=0;1;true"
            it[intPreferencesKey("new_words_per_day")] = 3
            it[intPreferencesKey("daily_goal_minutes")] = 5
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-02=300"
            it[stringPreferencesKey("streak_v1")] = "4;2026-11-02"
        }
        val before = original.data.first().asMap()
        val firstStore = ProgressStore(original, clock)
        val deck = firstStore.deck.first()
        assertEquals(2, DirectionalWorkload.of(words("boon"), deck, today, 0, TestMode.MIXED).waiting)
        assertEquals(before, original.data.first().asMap())

        firstStore.recordAnswer("boon", en, true)
        val after = original.data.first().asMap()
        before.forEach { (key, value) -> assertEquals("Existing $key was modified", value, after[key]) }
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        assertEquals(Stage.MAINTENANCE, saved.of("boon", en)?.stage)
        assertEquals(LocalDate.parse("2027-11-03"), saved.of("boon", en)?.due)
        assertEquals(LocalDate.parse("2026-03-01"), saved.of("boon", en)?.introducedOn)
        assertFalse(saved.isLearning("boon", en))
        assertEquals(pl, DirectionalWorkload.of(words("boon"), saved, today, 0, TestMode.MIXED).cards.single().direction)
        assertFalse(store.difficulty.first().isDifficult("boon", en))
        assertTrue(store.difficulty.first().isDifficult("boon", pl))
        assertEquals(0, saved.introducedOn(today))
        assertEquals(5, SettingsStore(dataStore).dailyGoalMinutes.first())
        assertEquals(300L, ScreenTimeLog(dataStore, clock).dailySeconds.first().secondsOn(today.minusDays(1)))
        assertEquals(Streak(4, today.minusDays(1)), ScreenTimeLog(dataStore, clock).streak.first())
    }

    @Test
    fun `version 1_7 directional retirement and pending learning survive maintenance grading and restart independently`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        val progressKey = stringPreferencesKey("directional_word_progress_v2")
        val difficultyKey = stringPreferencesKey("directional_difficulty_v2")
        original.edit {
            it[progressKey] =
                "ENGLISH_FIRST=boon=RET;-;2026-03-01,puddle=S1;2026-11-04;2026-11-03|puddle\n" +
                "POLISH_FIRST=boon=C90;2026-11-03;2026-03-01,puddle=S1;2026-11-04;2026-11-03|puddle"
            it[difficultyKey] = "ENGLISH_FIRST=boon=2;0;false\nPOLISH_FIRST=boon=0;1;true"
            it[stringPreferencesKey("word_progress_v1")] = "boon=S7;2026-10-20;2026-03-01"
            it[intPreferencesKey("new_words_per_day")] = 5
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-02=300"
        }
        val before = original.data.first().asMap()
        val firstStore = ProgressStore(original, clock)
        assertEquals(Stage.MAINTENANCE, firstStore.deck.first().of("boon", en)?.stage)
        assertEquals(before, original.data.first().asMap())

        firstStore.recordAnswer("boon", pl, true)
        assertEquals(Stage.C180, firstStore.deck.first().of("boon", pl)?.stage)
        assertEquals(LocalDate.parse("2027-05-02"), firstStore.deck.first().of("boon", pl)?.due)
        firstStore.recordAnswer("boon", en, false)
        clock.instant = Instant.parse("2027-05-02T10:00:00Z")
        firstStore.recordAnswer("boon", pl, true)
        val after = original.data.first().asMap()
        before.filterKeys { it != progressKey && it != difficultyKey }.forEach { (key, value) ->
            assertEquals("Existing $key was modified", value, after[key])
        }
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        assertEquals(Stage.C365, saved.of("boon", pl)?.stage)
        assertEquals(LocalDate.parse("2028-05-01"), saved.of("boon", pl)?.due)
        assertEquals(Stage.S1, saved.of("boon", en)?.stage)
        assertEquals(today.plusDays(1), saved.of("boon", en)?.due)
        assertFalse(saved.isLearning("boon", en))
        CardDirection.entries.forEach { direction ->
            assertTrue(saved.isLearning("puddle", direction))
            assertEquals(LocalDate.parse("2026-03-01"), saved.of("boon", direction)?.introducedOn)
        }
        assertEquals(1, saved.introducedOn(today))
        assertTrue(store.difficulty.first().isDifficult("boon", en))
        assertFalse(store.difficulty.first().isDifficult("boon", pl))
    }
}
