package com.krzyzowski.vocabboost.words

import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.session.PracticeDeck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DrivingVocabularyTest {
    private val originalIds = listOf(
        "dump", "dump-out", "dispose-of", "phone-battery", "household-waste", "daunting-task",
        "lynx", "pike", "locksmith", "fit-a-new-kitchen", "kitchen-fitter", "stained",
        "one-track-mind", "record-player", "beekeeper", "climb-the-ladder", "talk-shop", "busker",
        "tradesman", "graduate-from-university", "become-apparent", "come-back-and-bite-you-in-the-arse",
        "revise", "basic", "limited", "decent", "proficient", "shirk", "in-charge-of", "pull-something-off",
        "by-the-skin-of-your-teeth", "refer-to", "weekly-wage", "throw-a-sickie", "daunting", "cobbler",
        "cobble-something-together", "full-time", "good-mark", "turn-down", "hard-to-quit", "would",
        "might", "could", "retire", "routine-check-up", "listen-to-your-chest", "run-a-blood-test",
        "eye-chart", "donor", "run-a-series-of-medical-tests", "sweat-buckets", "strangle",
        "smother-a-fire", "stoke-a-fire", "suffocate", "stuffy-nose", "clutch", "physical-trauma",
        "lifeguard", "swelling", "inflammation", "shallow", "eat-shit", "rash", "road-rash", "lightheaded",
        "prescribe", "plaster-cast", "repeat-prescription", "bad-break-up", "exceed-a-dose", "dose-up",
        "overdose", "cut-something-out", "cut-down-on", "recover-from", "full-recovery", "no-pain-no-gain",
        "cleaver", "puncture", "let-go-of", "be-let-go", "cavity", "gold-filling", "stain", "dyes",
        "tooth-decay", "gums", "wear-and-tear",
    )

    private val addedIds = listOf(
        "overtake", "stinger", "stinging-remark", "spike-strip", "tyre-deflation-device",
        "when-the-petrol-light-came-on", "tow", "tug", "toll", "ignition", "turn-the-key", "halt",
        "bring-traffic-to-a-halt", "affect-the-plan", "drop-the-plan", "change-the-plan", "scrap-the-plan",
        "car-battery", "lose-charge", "blow-out-tyre", "flat-tyre", "sealant", "truck-mounted-crane",
        "jack", "lift-up", "spare-change", "spare-room", "spanner", "take-up-space",
        "at-the-side-of-the-road", "divert-traffic", "roadworks", "on-all-night", "raise-the-car",
        "lower-the-car", "unscrew", "bolts", "piggy-bank", "speed-bump", "break-down", "speed-camera",
        "built-up-area", "pull-over", "warning-triangle", "parking-ticket", "parking-meter",
        "the-lanes-are-merging", "hit-and-run", "windscreen-wiper", "get-a-lift", "give-someone-a-lift",
        "fill-up", "clutch-car", "hedgehog",
    )

    @Test
    fun `all ninety original word ids and their introduction order stay unchanged`() {
        assertEquals(90, originalIds.size)
        assertEquals(originalIds, VOCAB_WORDS.take(90).map { it.id })
    }

    @Test
    fun `the complete driving list is appended without replacing existing cards`() {
        assertEquals(54, addedIds.size)
        assertEquals(144, VOCAB_WORDS.size)
        assertEquals(addedIds, VOCAB_WORDS.drop(90).map { it.id })
    }

    @Test
    fun `repeated puncture and wear-and-tear entries do not create duplicate cards`() {
        listOf("puncture", "wear and tear").forEach { english ->
            assertEquals(1, VOCAB_WORDS.count { it.english == english })
        }
    }

    @Test
    fun `the car and gaming meanings of clutch are separate unambiguous cards`() {
        val gaming = VOCAB_WORDS.single { it.id == "clutch" }
        val car = VOCAB_WORDS.single { it.id == "clutch-car" }

        assertEquals("clutch (gaming)", gaming.english)
        assertTrue(gaming.polish.contains("slang gier"))
        assertEquals("clutch (car)", car.english)
        assertEquals("sprzęgło (w samochodzie)", car.polish)
    }

    @Test
    fun `existing schedules and difficulty survive while appended words start unlearned`() {
        val progress = DeckProgress.decode(
            "clutch=S3;2026-11-02;2026-10-30,puncture=S14;2026-11-02;2026-10-30," +
                "wear-and-tear=C60;2026-11-02;2026-10-30",
        )
        val difficulty = DifficultyProgress.decode("puncture=0;1;true")
        val practice = PracticeDeck(VOCAB_WORDS, progress)
        val workload = Workload.of(VOCAB_WORDS, progress, LocalDate.parse("2026-11-02"), newWordsPerDay = 8)

        assertEquals(Stage.S14, practice.stageAt(VOCAB_WORDS.indexOfFirst { it.id == "puncture" }))
        assertEquals(listOf("clutch", "puncture", "wear-and-tear"), workload.reviews.map { it.id })
        assertEquals(8, workload.newWords.size)
        assertEquals("puncture", PracticeDeck.difficult(VOCAB_WORDS, progress, difficulty).wordAt(0).id)
        assertEquals(1, difficulty.of("puncture")?.recoveries)
        addedIds.forEach { id ->
            assertNull("New word $id must not inherit another word's schedule", progress.of(id))
            assertFalse("New word $id must not inherit another word's difficulty", difficulty.isDifficult(id))
        }
    }
}
