package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.schedule.WordProgress
import com.krzyzowski.vocabboost.time.FakeClock
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ProgressStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())

    private val clock = FakeClock(Instant.parse("2026-10-30T10:00:00Z"), ZoneId.of("Europe/Warsaw"))

    private fun openStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    private val store by lazy { ProgressStore(openStore(dataStoreScope), clock) }

    @After
    fun closeDataStore() {
        dataStoreScope.cancel()
    }

    @Test
    fun `an answer is saved at once and is still there after the app restarts`() = runTest {
        val firstRun = Job()
        val before = ProgressStore(openStore(CoroutineScope(Dispatchers.IO + firstRun)), clock)
        before.recordAnswer("boon", knew = true)
        firstRun.cancelAndJoin()

        val saved = store.deck.first().of("boon")

        assertEquals(WordProgress(Stage.S1, LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-30")), saved)
    }

    @Test
    fun `words that were not answered keep their due date and stay new`() = runTest {
        store.recordAnswer("puddle", knew = true)
        clock.instant = Instant.parse("2026-11-02T10:00:00Z")

        store.recordAnswer("boon", knew = false)

        val deck = store.deck.first()
        assertEquals(
            WordProgress(Stage.S1, LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-30")),
            deck.of("puddle"),
        )
        assertEquals(
            WordProgress(Stage.S1, LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-02")),
            deck.of("boon"),
        )
        assertNull(deck.of("thorough"))
    }

    @Test
    fun `a word known at every review climbs the calendar-date ladder and continues with annual maintenance`() = runTest {
        val walk = listOf(
            Triple("2026-10-30", Stage.S1, "2026-10-31"),
            Triple("2026-10-31", Stage.S3, "2026-11-03"),
            Triple("2026-11-03", Stage.S7, "2026-11-10"),
            Triple("2026-11-10", Stage.S14, "2026-11-24"),
            Triple("2026-11-24", Stage.S30, "2026-12-24"),
            Triple("2026-12-24", Stage.C60, "2027-02-22"),
            Triple("2027-02-22", Stage.C90, "2027-05-23"),
            Triple("2027-05-23", Stage.C180, "2027-11-19"),
            Triple("2027-11-19", Stage.C365, "2028-11-18"),
            Triple("2028-11-18", Stage.MAINTENANCE, "2029-11-18"),
            Triple("2029-11-18", Stage.MAINTENANCE, "2030-11-18"),
        )

        walk.forEach { (answeredOn, stage, due) ->
            clock.instant = Instant.parse("${answeredOn}T10:00:00Z")
            store.recordAnswer("boon", knew = true)

            val progress = store.deck.first().of("boon")
            assertEquals("stage after answering on $answeredOn", stage, progress?.stage)
            assertEquals("due after answering on $answeredOn", LocalDate.parse(due), progress?.due)
        }
    }

    @Test
    fun `a word answered late is scheduled from the day it was answered`() = runTest {
        store.recordAnswer("boon", knew = true)
        clock.instant = Instant.parse("2026-11-12T10:00:00Z")

        store.recordAnswer("boon", knew = true)

        assertEquals(
            WordProgress(Stage.S3, LocalDate.parse("2026-11-15"), LocalDate.parse("2026-10-30")),
            store.deck.first().of("boon"),
        )
    }

    @Test
    fun `a due date stays a calendar day when the clocks go forward in between`() = runTest {
        clock.instant = Instant.parse("2026-03-20T10:00:00Z")
        store.recordAnswer("boon", knew = true)
        clock.instant = Instant.parse("2026-03-21T10:00:00Z")
        store.recordAnswer("boon", knew = true)

        // 23:30 in Warsaw on 24 March. Seven 24-hour days later it would already be 1 April local time.
        clock.instant = Instant.parse("2026-03-24T22:30:00Z")
        store.recordAnswer("boon", knew = true)

        assertEquals(
            WordProgress(Stage.S7, LocalDate.parse("2026-03-31"), LocalDate.parse("2026-03-20")),
            store.deck.first().of("boon"),
        )
    }

    @Test
    fun `the day of an answer follows the phone's time zone at that moment`() = runTest {
        clock.instant = Instant.parse("2026-06-10T23:30:00Z")
        clock.zoneId = ZoneId.of("Europe/Warsaw")
        store.recordAnswer("boon", knew = true)

        clock.zoneId = ZoneId.of("America/New_York")
        store.recordAnswer("puddle", knew = true)

        val deck = store.deck.first()
        assertEquals(LocalDate.parse("2026-06-12"), deck.of("boon")?.due)
        assertEquals(LocalDate.parse("2026-06-11"), deck.of("puddle")?.due)
    }

    @Test
    fun `answers given at the same moment are all saved`() = runTest {
        val ids = (1..20).map { "word-$it" }

        coroutineScope { ids.map { id -> async { store.recordAnswer(id, knew = true) } }.awaitAll() }

        val deck = store.deck.first()
        assertEquals(emptyList<String>(), ids.filter { deck.of(it) == null })
    }
}
