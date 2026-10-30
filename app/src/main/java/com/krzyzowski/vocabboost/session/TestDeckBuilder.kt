package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Workload
import kotlin.random.Random

class TestDeckBuilder(private val random: Random) {

    fun build(workload: Workload, deck: DeckProgress): List<TestCard> {
        val reviews = workload.reviews.map { word ->
            val reversed = deck.of(word.id)?.stage?.reversible == true && random.nextBoolean()
            TestCard(word, isNew = false, if (reversed) CardDirection.POLISH_FIRST else CardDirection.ENGLISH_FIRST)
        }
        return reviews + workload.newWords.map { TestCard(it, isNew = true, CardDirection.ENGLISH_FIRST) }
    }
}
