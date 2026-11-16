package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWord

enum class CardDirection {
    ENGLISH_FIRST,
    POLISH_FIRST;

    fun front(word: VocabWord): String = when (this) {
        ENGLISH_FIRST -> word.english
        POLISH_FIRST -> word.polish
    }

    /** The translation the user is expected to produce for the [front] text. */
    fun expected(word: VocabWord): String = when (this) {
        ENGLISH_FIRST -> word.polish
        POLISH_FIRST -> word.english
    }
}
