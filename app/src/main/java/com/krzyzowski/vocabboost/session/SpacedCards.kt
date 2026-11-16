package com.krzyzowski.vocabboost.session

/**
 * Planned order of the cards still to be shown in a test.
 *
 * [deferred] holds any input card the planner did not place in [ready]. It is empty by design:
 * when no card can keep the minimum gap, the planner falls back to the best gap available
 * instead of holding cards back. It is still reported from the planner's leftovers, so a
 * regression that stops planning early is visible to callers and tests.
 */
data class SpacedCards(val ready: List<TestCard>, val deferred: List<TestCard> = emptyList()) {
    companion object {
        const val MIN_OTHER_CARDS = 3
        const val PREFERRED_OTHER_CARDS = 4

        fun of(cards: List<TestCard>, shown: List<TestCard> = emptyList()): SpacedCards {
            val pending = cards.toMutableList()
            val ready = mutableListOf<TestCard>()
            val recent = shown.takeLast(PREFERRED_OTHER_CARDS).map { it.word.id }.toMutableList()
            while (pending.isNotEmpty()) {
                val minimumBlocked = recent.takeLast(MIN_OTHER_CARDS).toSet()
                val preferredBlocked = recent.toSet()
                val reviews = pending.indices.filter { !pending[it].isNew && pending[it].word.id !in minimumBlocked }
                val newCards = pending.indices.filter { pending[it].isNew && pending[it].word.id !in minimumBlocked }
                val eligible = reviews.ifEmpty { newCards }
                if (eligible.isEmpty()) {
                    // Not enough material for the minimum gap: show the word seen least recently.
                    val card = pending.removeAt(leastRecentIndex(pending, recent))
                    ready += card
                    recent += card.word.id
                    if (recent.size > PREFERRED_OTHER_CARDS) recent.removeAt(0)
                    continue
                }
                val counts = pending.groupingBy { it.word.id }.eachCount()
                // Keep a minimum-gap slot when waiting longer would strand another repeat.
                val urgent = eligible.filter {
                    (counts.getValue(pending[it].word.id) - 1) * (MIN_OTHER_CARDS + 1) >= pending.size - 1
                }
                val candidates = urgent.ifEmpty {
                    eligible.filter { pending[it].word.id !in preferredBlocked }.ifEmpty { eligible }
                }
                val index = candidates.maxBy { counts.getValue(pending[it].word.id) }
                val card = pending.removeAt(index)
                ready += card
                recent += card.word.id
                if (recent.size > PREFERRED_OTHER_CARDS) recent.removeAt(0)
            }
            return SpacedCards(ready, pending)
        }

        /**
         * Index of the pending card whose word was shown least recently; within that word a
         * review comes before a new card, then the supplied order wins.
         */
        private fun leastRecentIndex(pending: List<TestCard>, recent: List<String>): Int {
            val oldestWord = pending.map { it.word.id }.distinct().minBy { recent.lastIndexOf(it) }
            val ofWord = pending.indices.filter { pending[it].word.id == oldestWord }
            return ofWord.firstOrNull { !pending[it].isNew } ?: ofWord.first()
        }
    }
}
