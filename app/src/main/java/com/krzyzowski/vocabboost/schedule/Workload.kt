package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.words.VocabWord
import java.time.LocalDate

data class Workload(val reviews: List<VocabWord>, val newWords: List<VocabWord>) {
    val waiting: Int
        get() = reviews.size + newWords.size

    companion object {
        const val MAX_NEW_WORDS_PER_DAY = 5

        fun of(words: List<VocabWord>, deck: DeckProgress, day: LocalDate, newWordsPerDay: Int): Workload {
            val dailyLimit = newWordsPerDay.coerceIn(0, MAX_NEW_WORDS_PER_DAY)
            val newQuota = (dailyLimit - deck.introducedOn(day)).coerceAtLeast(0)
            return Workload(
                reviews = words
                    .filter { deck.of(it.id)?.isDueBy(day) == true }
                    .sortedBy { deck.of(it.id)?.due },
                newWords = words.filter { deck.of(it.id) == null }.take(newQuota),
            )
        }
    }
}
