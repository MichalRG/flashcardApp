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
import com.krzyzowski.vocabboost.session.completedOf
import com.krzyzowski.vocabboost.session.remainingOf
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
    fun `mature mixed directions are all answered in one test without touching unrelated settings`() = runTest {
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
        assertEquals(6, session.cards.size)
        assertEquals(before, original.data.first().asMap())
        var steps = 0
        while (!session.isOver) {
            assertTrue("Unexpected endless test", steps++ < 50)
            val card = requireNotNull(session.current)
            firstStore.recordAnswer(card.word.id, card.direction, true)
            session = session.answer(knew = true, timeUp = false)
        }
        assertEquals(6, session.answered)
        assertTrue(completedOf(session))
        assertTrue(session.deferredCards.isEmpty())
        val after = original.data.first().asMap()
        before.forEach { (key, value) -> assertEquals("Existing $key was modified", value, after[key]) }
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        list.forEach { word ->
            CardDirection.entries.forEach { direction ->
                assertEquals(Stage.S14, saved.of(word.id, direction)?.stage)
                assertEquals(today.plusDays(14), saved.of(word.id, direction)?.due)
            }
        }
        // The legacy recovery count of 1 plus this "Knew it" completes the recovery in each direction.
        assertFalse(store.difficulty.first().isDifficult("a", CardDirection.ENGLISH_FIRST))
        assertFalse(store.difficulty.first().isDifficult("a", CardDirection.POLISH_FIRST))
        val waiting = DirectionalWorkload.of(list, saved, today, 0, TestMode.MIXED)
        assertEquals(0, waiting.waiting)
        assertEquals(0, saved.introducedOn(today))
    }

    @Test
    fun `new mixed directions finish initial learning in one test`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        val firstStore = ProgressStore(original, clock)
        var session = TestSession.start(
            DirectionalWorkload.of(list, firstStore.deck.first(), today, 3, TestMode.MIXED).cards,
        )
        assertEquals(6, session.cards.size)
        var steps = 0
        while (!session.isOver) {
            assertTrue("Unexpected endless test", steps++ < 50)
            val card = requireNotNull(session.current)
            firstStore.recordAnswer(card.word.id, card.direction, true)
            session = session.answer(knew = true, timeUp = false)
        }
        assertEquals(6, session.answered)
        assertTrue(completedOf(session))
        assertTrue(session.pendingLearning.isEmpty())
        firstRun.cancelAndJoin()

        val saved = store.deck.first()
        assertEquals(3, saved.introducedOn(today))
        list.forEach { word ->
            CardDirection.entries.forEach { direction ->
                assertFalse(saved.isLearning(word.id, direction))
                assertEquals(Stage.S1, saved.of(word.id, direction)?.stage)
                assertEquals(today.plusDays(1), saved.of(word.id, direction)?.due)
            }
        }
        val waiting = DirectionalWorkload.of(list, saved, today, 3, TestMode.MIXED)
        assertEquals(0, waiting.waiting)
        assertTrue(waiting.newWords.isEmpty())
    }

    @Test
    fun `ending a mature test early leaves exactly the unanswered directions for the next test`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        original.edit {
            it[stringPreferencesKey("word_progress_v1")] =
                list.joinToString(",") { word -> "${word.id}=S7;2026-11-03;2026-10-20" }
        }
        val firstStore = ProgressStore(original, clock)
        var session = TestSession.start(
            DirectionalWorkload.of(list, firstStore.deck.first(), today, 0, TestMode.MIXED).cards,
        )
        repeat(2) {
            val card = requireNotNull(session.current)
            firstStore.recordAnswer(card.word.id, card.direction, true)
            session = session.answer(knew = true, timeUp = false)
        }
        val ended = session.end()
        val owed = ended.cards.drop(ended.position).map { it.key }
        firstRun.cancelAndJoin()

        assertFalse(completedOf(ended))
        assertEquals(4, remainingOf(ended))
        val reopened = DirectionalWorkload.of(list, store.deck.first(), today, 0, TestMode.MIXED)
        assertEquals(remainingOf(ended), reopened.waiting)
        assertEquals(owed.toSet(), reopened.cards.map { it.key }.toSet())
    }

    @Test
    fun `ending a new-word test early leaves exactly the unanswered directions for the next test`() = runTest {
        val firstRun = Job()
        val original = openStore(CoroutineScope(Dispatchers.IO + firstRun))
        val firstStore = ProgressStore(original, clock)
        var session = TestSession.start(
            DirectionalWorkload.of(list, firstStore.deck.first(), today, 3, TestMode.MIXED).cards,
        )
        repeat(2) {
            val card = requireNotNull(session.current)
            firstStore.recordAnswer(card.word.id, card.direction, true)
            session = session.answer(knew = true, timeUp = false)
        }
        val ended = session.end()
        val owed = ended.cards.drop(ended.position).map { it.key }
        firstRun.cancelAndJoin()

        assertFalse(completedOf(ended))
        assertEquals(4, remainingOf(ended))
        val reopened = DirectionalWorkload.of(list, store.deck.first(), today, 3, TestMode.MIXED)
        assertEquals(remainingOf(ended), reopened.waiting)
        assertEquals(owed.toSet(), reopened.cards.map { it.key }.toSet())
    }
}
