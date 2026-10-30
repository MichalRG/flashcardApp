package com.krzyzowski.vocabboost.session

data class TestSession(
    val cards: List<TestCard>,
    val position: Int = 0,
    val knew: Int = 0,
    val didNotKnow: Int = 0,
    val stopped: Boolean = false,
) {
    val answered: Int
        get() = knew + didNotKnow

    val isOver: Boolean
        get() = stopped || position >= cards.size

    val current: TestCard?
        get() = if (isOver) null else cards[position]

    fun answer(knew: Boolean, timeUp: Boolean): TestSession {
        if (isOver) return this
        return copy(
            position = position + 1,
            knew = this.knew + if (knew) 1 else 0,
            didNotKnow = didNotKnow + if (knew) 0 else 1,
            stopped = timeUp,
        )
    }

    fun end(): TestSession = copy(stopped = true)
}
