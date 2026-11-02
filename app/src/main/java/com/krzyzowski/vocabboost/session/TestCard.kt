package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWord

data class TestCard(
    val word: VocabWord,
    val isNew: Boolean,
    val direction: CardDirection,
    val isLearning: Boolean = isNew,
) {
    val key: LearningKey
        get() = LearningKey(word.id, direction)
}
