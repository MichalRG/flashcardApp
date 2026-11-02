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
        val nextCards = if (card.isLearning && !knew) withLearningRetry(card) else cards
        val nextPosition = position + 1
        return copy(
            cards = nextCards,
            position = nextPosition,
            knew = this.knew + if (knew) 1 else 0,
            didNotKnow = didNotKnow + if (knew) 0 else 1,
            pendingLearning = if (card.isLearning && knew) pendingLearning - card.key else pendingLearning,
            awaitingTimeChoice = timeUp && !continuedPastLimit && nextPosition < nextCards.size,
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
            if (intervening == 3) shorterGap = index + 1
            if (intervening == 4) {
                return cards.toMutableList().apply { add(index + 1, card.copy(isNew = false)) }
            }
        }
        return shorterGap?.let { insertion ->
            cards.toMutableList().apply { add(insertion, card.copy(isNew = false)) }
        } ?: cards
    }
}
