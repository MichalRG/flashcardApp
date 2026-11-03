package com.krzyzowski.vocabboost.session

data class TestSession(
    val cards: List<TestCard>,
    val position: Int = 0,
    val knew: Int = 0,
    val didNotKnow: Int = 0,
    val stopped: Boolean = false,
    val pendingLearning: Set<LearningKey> = cards.filter { it.isLearning }.map { it.key }.toSet(),
    val awaitingTimeChoice: Boolean = false,
    val continuedPastLimit: Boolean = false,
    val deferredCards: List<TestCard> = emptyList(),
) {
    val answered: Int
        get() = knew + didNotKnow

    val isOver: Boolean
        get() = stopped || position >= cards.size

    val current: TestCard?
        get() = if (isOver) null else cards[position]

    fun answer(knew: Boolean, timeUp: Boolean): TestSession {
        if (isOver || awaitingTimeChoice) return this
        val card = requireNotNull(current)
        val withRetry = if (card.isLearning && !knew) withLearningRetry(card) else cards
        val nextPosition = position + 1
        val shown = withRetry.take(nextPosition)
        val remaining = SpacedCards.of(withRetry.drop(nextPosition) + deferredCards, shown)
        val nextCards = shown + remaining.ready
        return copy(
            cards = nextCards,
            position = nextPosition,
            knew = this.knew + if (knew) 1 else 0,
            didNotKnow = didNotKnow + if (knew) 0 else 1,
            pendingLearning = if (card.isLearning && knew) pendingLearning - card.key else pendingLearning,
            awaitingTimeChoice = timeUp && !continuedPastLimit && nextPosition < nextCards.size,
            deferredCards = remaining.deferred,
        )
    }

    fun continueLearning(): TestSession =
        if (awaitingTimeChoice) copy(awaitingTimeChoice = false, continuedPastLimit = true) else this

    fun end(): TestSession = copy(stopped = true, awaitingTimeChoice = false)

    private fun withLearningRetry(card: TestCard): List<TestCard> {
        var intervening = 0
        var shorterGap: Int? = null
        for (index in position + 1 until cards.size) {
            intervening = if (cards[index].word.id == card.word.id) 0 else intervening + 1
            if (intervening == SpacedCards.MIN_OTHER_CARDS) shorterGap = index + 1
            if (intervening == SpacedCards.PREFERRED_OTHER_CARDS) {
                return cards.toMutableList().apply { add(index + 1, card.copy(isNew = false)) }
            }
        }
        return shorterGap?.let { insertion ->
            cards.toMutableList().apply { add(insertion, card.copy(isNew = false)) }
        } ?: cards
    }

    companion object {
        fun start(cards: List<TestCard>): TestSession {
            val spaced = SpacedCards.of(cards)
            return TestSession(
                cards = spaced.ready,
                deferredCards = spaced.deferred,
                pendingLearning = cards.filter { it.isLearning }.map { it.key }.toSet(),
            )
        }
    }
}
