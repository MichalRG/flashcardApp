package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.TestCard
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.words.VocabWord
import java.time.LocalDate

data class DirectionalWorkload(
    val reviews: List<TestCard>,
    val newWords: List<VocabWord>,
    val newCards: List<TestCard>,
) {
    val cards: List<TestCard>
        get() = reviews + newCards

    val waiting: Int
        get() = reviews.size + newCards.size

    companion object {
        fun of(
            words: List<VocabWord>,
            deck: DirectionalProgress,
            day: LocalDate,
            newWordsPerDay: Int,
            mode: TestMode,
        ): DirectionalWorkload {
            val directions = mode.directions
            val reviews = words.flatMap { word ->
                directions.mapNotNull { direction ->
                    val progress = deck.of(word.id, direction) ?: return@mapNotNull null
                    val learning = deck.isLearning(word.id, direction)
                    if (!learning && !progress.isDueBy(day)) return@mapNotNull null
                    TestCard(word, isNew = false, direction, isLearning = learning)
                }
            }.sortedBy {
                val progress = requireNotNull(deck.of(it.word.id, it.direction))
                if (it.isLearning) progress.introducedOn else progress.due
            }
            val quota = (newWordsPerDay.coerceIn(0, Workload.MAX_NEW_WORDS_PER_DAY) -
                deck.introducedOn(day)).coerceAtLeast(0)
            val newWords = words.filterNot { deck.hasWord(it.id) }.take(quota)
            val newCards = directions.flatMap { direction ->
                newWords.map { TestCard(it, isNew = true, direction, isLearning = true) }
            }
            return DirectionalWorkload(reviews, newWords, newCards)
        }
    }
}
