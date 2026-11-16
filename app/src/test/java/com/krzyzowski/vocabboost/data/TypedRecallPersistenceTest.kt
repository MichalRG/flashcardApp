package com.krzyzowski.vocabboost.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.schedule.requiresTypedRecall
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.time.FakeClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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

class TypedRecallPersistenceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val clock = FakeClock(Instant.parse("2026-11-02T10:00:00Z"))
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }
    }
    private val store by lazy { ProgressStore(dataStore, clock) }
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST

    @After
    fun closeStore() {
        scope.cancel()
    }

    @Test
    fun `difficulty read right after recording shows the new streak and the requirement`() = runTest {
        store.recordAnswer("boon", en, false)
        store.recordAnswer("boon", en, false)
        assertFalse(store.difficulty.first().requiresTypedRecall("boon", en))

        store.recordAnswer("boon", en, false)
        val gated = store.difficulty.first()
        assertTrue(gated.requiresTypedRecall("boon", en))
        assertFalse(gated.requiresTypedRecall("boon", pl))

        store.recordAnswer("boon", en, true)
        assertFalse(store.difficulty.first().requiresTypedRecall("boon", en))
    }

    @Test
    fun `a miss after recovery does not require typing again`() = runTest {
        repeat(3) { store.recordAnswer("boon", en, false) }
        store.recordAnswer("boon", en, true)
        store.recordAnswer("boon", en, false)

        assertFalse(store.difficulty.first().requiresTypedRecall("boon", en))
    }

    @Test
    fun `legacy difficulty with three misses requires typing in both directions without a migration`() = runTest {
        val legacy = (1..3).fold(DifficultyProgress.NONE) { progress, _ -> progress.recordAnswer("boon", false) }
        dataStore.edit { it[stringPreferencesKey("difficulty_progress_v1")] = legacy.encode() }
        val before = dataStore.data.first().asMap()

        val difficulty = store.difficulty.first()

        assertTrue(difficulty.requiresTypedRecall("boon", en))
        assertTrue(difficulty.requiresTypedRecall("boon", pl))
        assertEquals(before, dataStore.data.first().asMap())
    }

    @Test
    fun `recording answers writes no new storage key for the typed recall requirement`() = runTest {
        repeat(3) { store.recordAnswer("boon", en, false) }

        val keys = dataStore.data.first().asMap().keys.map { it.name }.toSet()

        assertEquals(setOf("directional_word_progress_v2", "directional_difficulty_v2"), keys)
    }
}
