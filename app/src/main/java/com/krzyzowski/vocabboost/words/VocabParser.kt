package com.krzyzowski.vocabboost.words

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class VocabParser {
    fun parseAll(files: List<VocabFile>): List<VocabWord> {
        val located = files.sortedBy { it.name }.flatMap { file -> parse(file).map { word -> file.name to word } }
        val repeated = located.groupBy({ (_, word) -> word.id }, { (name, _) -> name }).filterValues { it.size > 1 }
        require(repeated.isEmpty()) {
            repeated.entries.joinToString("; ") { (id, names) -> "id $id is used more than once (${names.joinToString()})" }
        }
        return located.map { (_, word) -> word }
    }

    fun parse(file: VocabFile): List<VocabWord> {
        val name = file.name
        require(name.endsWith(".json") && SLUG.matches(name.removeSuffix(".json"))) {
            "$name: the file name must be a lower-case slug ending in .json, such as 03-kitchen.json"
        }
        val json = try {
            JSONObject(file.text)
        } catch (e: JSONException) {
            throw IllegalArgumentException("$name: not a valid JSON object (${e.message})", e)
        }
        json.requireKnownKeys(name, FILE_KEYS)
        val items = json.opt("words")
        require(items is JSONArray && items.length() > 0) { "$name: words must be a non-empty array" }
        return List(items.length()) { index -> word("$name word ${index + 1}", items.opt(index)) }
    }

    private fun word(where: String, item: Any?): VocabWord {
        require(item is JSONObject) { "$where: must be an object" }
        item.requireKnownKeys(where, WORD_KEYS)
        val id = item.requiredString(where, "id")
        require(SLUG.matches(id)) { "$where: id $id must be a lower-case slug such as take-for-granted" }
        val english = item.requiredString(where, "english")
        val example = item.requiredString(where, "example")
        return VocabWord(
            id = id,
            english = english,
            example = example,
            polish = item.requiredString(where, "polish"),
            contextAnswer = item.optionalString(where, "contextAnswer") ?: english,
            contextSentence = item.optionalString(where, "contextSentence") ?: example,
        )
    }

    private fun JSONObject.requiredString(where: String, key: String): String =
        optionalString(where, key) ?: throw IllegalArgumentException("$where: $key must be a non-blank string")

    private fun JSONObject.optionalString(where: String, key: String): String? {
        if (!has(key)) return null
        val value = opt(key)
        require(value is String && value.isNotBlank()) { "$where: $key must be a non-blank string" }
        return value
    }

    private fun JSONObject.requireKnownKeys(where: String, allowed: List<String>) {
        val unknown = keys().asSequence().filterNot { it in allowed }.sorted().toList()
        require(unknown.isEmpty()) {
            "$where: unknown field ${unknown.joinToString()}; allowed fields are ${allowed.joinToString()}"
        }
    }

    private companion object {
        val SLUG = Regex("[a-z0-9]+(-[a-z0-9]+)*")
        val FILE_KEYS = listOf("words")
        val WORD_KEYS = listOf("id", "english", "example", "polish", "contextAnswer", "contextSentence")
    }
}
