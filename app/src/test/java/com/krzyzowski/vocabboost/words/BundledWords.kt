package com.krzyzowski.vocabboost.words

import java.io.File

val BUNDLED_WORD_FILES: List<VocabFile> = File("src/main/assets/words").listFiles().orEmpty()
    .filterNot { it.name.startsWith(".") }
    .map { VocabFile(it.name, it.readText()) }

val BUNDLED_WORDS: List<VocabWord> = VocabParser().parseAll(BUNDLED_WORD_FILES)
