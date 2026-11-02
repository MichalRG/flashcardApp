package com.krzyzowski.vocabboost.session

enum class TestMode {
    MIXED, POLISH_FIRST, ENGLISH_FIRST;

    val directions: List<CardDirection>
        get() = when (this) {
            MIXED -> listOf(CardDirection.POLISH_FIRST, CardDirection.ENGLISH_FIRST)
            POLISH_FIRST -> listOf(CardDirection.POLISH_FIRST)
            ENGLISH_FIRST -> listOf(CardDirection.ENGLISH_FIRST)
        }
}
