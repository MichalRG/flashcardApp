package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.Workload
import kotlin.random.Random

class TestDeckBuilder(private val random: Random) {

    fun build(workload: Workload, mode: TestMode): List<TestCard> {
        val directions = when (mode) {
            TestMode.MIXED -> {
                val polishCount = (workload.waiting * 2 + 1) / 3
                (List(polishCount) { CardDirection.POLISH_FIRST } +
                    List(workload.waiting - polishCount) { CardDirection.ENGLISH_FIRST }).shuffled(random)
            }
            TestMode.POLISH_FIRST -> List(workload.waiting) { CardDirection.POLISH_FIRST }
            TestMode.ENGLISH_FIRST -> List(workload.waiting) { CardDirection.ENGLISH_FIRST }
        }
        return (workload.reviews + workload.newWords).mapIndexed { index, word ->
            TestCard(word, isNew = index >= workload.reviews.size, direction = directions[index])
        }
    }
}
