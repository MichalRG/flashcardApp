package com.krzyzowski.vocabboost.words

object VocabWordBuilder {
    fun word(
        id: String,
        english: String = id,
        example: String = "A sentence that uses the word.",
        polish: String = "polskie tłumaczenie",
    ) = VocabWord(id = id, english = english, example = example, polish = polish)

    fun words(vararg ids: String): List<VocabWord> = ids.map { word(it) }
}
