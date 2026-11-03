package com.krzyzowski.vocabboost.words

data class VocabWord(
    val id: String,
    val english: String,
    val example: String,
    val polish: String,
    val contextAnswer: String = english,
    val contextSentence: String = example,
)
