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
    /**
     * Learning directions still owed in this test that are no longer queued at or after
     * [position], i.e. work that was held back or dropped instead of being served.
     *
     * Directions that cannot keep the preferred gap are shown in the same test with the largest
     * gap available and missed retries are always re-queued, so this is empty by design. It is
     * derived from [pendingLearning] and [cards] rather than hard-coded, so a regression that
     * strands learning work shows up here instead of silently ending the test early.
     */
    val deferredCards: List<TestCard>
        get() {
            val queued = cards.drop(position).map { it.key }.toSet()
            return pendingLearning.filterNot { it in queued }
                .mapNotNull { key -> cards.lastOrNull { it.key == key } }
        }

    /**
     * Unanswered cards still owed in this test: the card on screen, later queued cards and
     * learning retries. A pending learning key is represented by its queued retry, not counted again.
     */
    val remaining: Int
        get() = (cards.size - position).coerceAtLeast(0)

    /** True only when nothing is left to do in this test, whether or not it was stopped. */
    val isCompleted: Boolean
        get() = remaining == 0

    val answered: Int
        get() = knew + didNotKnow

    val isOver: Boolean
        get() = stopped || remaining == 0

    val current: TestCard?
        get() = if (isOver) null else cards[position]

    fun answer(knew: Boolean, timeUp: Boolean): TestSession {
        if (isOver || awaitingTimeChoice) return this
        val card = requireNotNull(current)
        val withRetry = if (card.isLearning && !knew) withLearningRetry(card) else cards
        val nextPosition = position + 1
        val shown = withRetry.take(nextPosition)
        val planned = SpacedCards.of(withRetry.drop(nextPosition), shown)
        val nextCards = shown + planned.ready
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
            if (intervening == SpacedCards.MIN_OTHER_CARDS) shorterGap = index + 1
            if (intervening == SpacedCards.PREFERRED_OTHER_CARDS) {
                return cards.toMutableList().apply { add(index + 1, card.copy(isNew = false)) }
            }
        }
        // Without room for the minimum gap the retry goes last: the largest gap available,
        // or the very next card when nothing else is left. It is never dropped.
        val insertion = shorterGap ?: cards.size
        return cards.toMutableList().apply { add(insertion, card.copy(isNew = false)) }
    }

    companion object {
        fun start(cards: List<TestCard>): TestSession {
            val spaced = SpacedCards.of(cards)
            return TestSession(
                cards = spaced.ready,
                pendingLearning = cards.filter { it.isLearning }.map { it.key }.toSet(),
            )
        }
    }
}
