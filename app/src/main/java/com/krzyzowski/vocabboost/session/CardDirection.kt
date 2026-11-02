package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWord

enum class CardDirection {
    ENGLISH_FIRST,
    POLISH_FIRST;

    fun front(word: VocabWord): String = when (this) {
        ENGLISH_FIRST -> word.english
        POLISH_FIRST -> word.polish
    }
}
