package com.krzyzowski.vocabboost.words

import android.content.res.AssetManager

class AssetVocabSource(private val assets: AssetManager, private val parser: VocabParser) {
    fun load(): List<VocabWord> = parser.parseAll(
        assets.list(DIRECTORY).orEmpty().map { name ->
            VocabFile(name, assets.open("$DIRECTORY/$name").bufferedReader().use { it.readText() })
        },
    )

    private companion object {
        const val DIRECTORY = "words"
    }
}
