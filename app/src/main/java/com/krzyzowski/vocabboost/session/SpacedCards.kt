package com.krzyzowski.vocabboost.session

data class SpacedCards(val ready: List<TestCard>, val deferred: List<TestCard>) {
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
                if (eligible.isEmpty()) break
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
    }
}
