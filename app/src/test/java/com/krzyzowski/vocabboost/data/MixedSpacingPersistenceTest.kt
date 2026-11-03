package com.krzyzowski.vocabboost.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DirectionalWorkload
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.session.TestSession
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

class MixedSpacingPersistenceTest {
    @get:Rule
    val folder = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val clock = FakeClock(Instant.parse("2026-11-03T10:00:00Z"))
    private val today = LocalDate.parse("2026-11-03")
    private val list = words("a", "b", "c")
    private val dataStore by lazy { openStore(scope) }
    private val store by lazy { ProgressStore(dataStore, clock) }

    private fun openStore(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    @After
    fun closeStore() {
        scope.cancel()
    }

    @Test
    fun `postponed mature directions survive restart with their original dates difficulty and all settings intact`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        original.edit {
            it[stringPreferencesKey("word_progress_v1")] =
                list.joinToString(",") { word -> "${word.id}=S7;2026-11-03;2026-10-20" }
            it[stringPreferencesKey("difficulty_progress_v1")] = "a=0;1;true"
            it[intPreferencesKey("daily_goal_minutes")] = 5
            it[stringPreferencesKey("daily_seconds_v1")] = "2026-11-02=300"
            it[stringPreferencesKey("streak_v1")] = "4;2026-11-02"
        }
        val before = original.data.first().asMap()
        val firstStore = ProgressStore(original, clock)
        val workload = DirectionalWorkload.of(list, firstStore.deck.first(), today, 0, TestMode.MIXED)
        var session = TestSession.start(workload.cards)
        assertEquals(6, workload.waiting)
        assertEquals(3, session.cards.size)
        assertEquals(before, original.data.first().asMap())
        while (!session.isOver) {
            val card = requireNotNull(session.current)
            firstStore.recordAnswer(card.word.id, card.direction, true)
            session = session.answer(knew = true, timeUp = false)
        }
        assertEquals(3, session.answered)
        assertEquals(3, session.deferredCards.size)
        val after = original.data.first().asMap()
        before.forEach { (key, value) -> assertEquals("Existing $key was modified", value, after[key]) }
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        list.forEach { word ->
            assertEquals(Stage.S14, saved.of(word.id, CardDirection.POLISH_FIRST)?.stage)
            assertEquals(today.plusDays(14), saved.of(word.id, CardDirection.POLISH_FIRST)?.due)
            assertEquals(Stage.S7, saved.of(word.id, CardDirection.ENGLISH_FIRST)?.stage)
            assertEquals(today, saved.of(word.id, CardDirection.ENGLISH_FIRST)?.due)
        }
        assertTrue(store.difficulty.first().isDifficult("a", CardDirection.ENGLISH_FIRST))
        assertFalse(store.difficulty.first().isDifficult("a", CardDirection.POLISH_FIRST))
        val waiting = DirectionalWorkload.of(list, saved, today, 0, TestMode.MIXED)
        assertEquals(3, waiting.waiting)
        assertTrue(waiting.cards.all { it.direction == CardDirection.ENGLISH_FIRST })
        assertEquals(3, TestSession.start(waiting.cards).cards.size)
        assertEquals(0, saved.introducedOn(today))
    }

    @Test
    fun `postponed new directions stay in initial learning after restart without consuming another word slot`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        val firstStore = ProgressStore(original, clock)
        var session = TestSession.start(
            DirectionalWorkload.of(list, firstStore.deck.first(), today, 3, TestMode.MIXED).cards,
        )
        while (!session.isOver) {
            val card = requireNotNull(session.current)
            firstStore.recordAnswer(card.word.id, card.direction, true)
            session = session.answer(knew = true, timeUp = false)
        }
        assertEquals(3, session.answered)
        assertEquals(3, session.pendingLearning.size)
        assertEquals(session.pendingLearning, session.deferredCards.map { it.key }.toSet())
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        assertEquals(3, saved.introducedOn(today))
        list.forEach { word ->
            assertFalse(saved.isLearning(word.id, CardDirection.POLISH_FIRST))
            assertTrue(saved.isLearning(word.id, CardDirection.ENGLISH_FIRST))
            assertEquals(Stage.S1, saved.of(word.id, CardDirection.POLISH_FIRST)?.stage)
            assertEquals(today.plusDays(1), saved.of(word.id, CardDirection.POLISH_FIRST)?.due)
        }
        val waiting = DirectionalWorkload.of(list, saved, today, 3, TestMode.MIXED)
        assertEquals(0, waiting.newWords.size)
        assertEquals(3, waiting.waiting)
        assertTrue(waiting.cards.all { it.isLearning && !it.isNew && it.direction == CardDirection.ENGLISH_FIRST })
    }
}
