package com.krzyzowski.vocabboost.words

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabParserTest {
    private val parser = VocabParser()

    private fun jsonObject(defaults: Map<String, String>, overrides: Array<out Pair<String, String?>>): String {
        val fields = LinkedHashMap(defaults)
        overrides.forEach { (key, value) -> if (value == null) fields.remove(key) else fields[key] = value }
        return fields.entries.joinToString(",", "{", "}") { (key, value) -> "\"$key\": $value" }
    }

    private fun word(vararg overrides: Pair<String, String?>): String = jsonObject(
        mapOf(
            "id" to "\"overtake\"",
            "english" to "\"overtake\"",
            "example" to "\"I waited before overtaking the lorry.\"",
            "polish" to "\"wyprzedzać\"",
        ),
        overrides,
    )

    private fun file(vararg overrides: Pair<String, String?>): String =
        jsonObject(mapOf("words" to "[${word()}]"), overrides)

    private fun wordsFile(vararg words: String): String = file("words" to words.joinToString(",", "[", "]"))

    private fun failure(block: () -> Unit): String? = try {
        block()
        null
    } catch (e: IllegalArgumentException) {
        e.message.orEmpty()
    }

    private fun assertRejected(name: String, text: String, mention: String, rule: String) {
        val message = failure { parser.parse(VocabFile(name, text)) }
        assertNotNull("accepted a file that $rule", message)
        assertTrue("$rule: message '$message' should name '$name'", message!!.contains(name))
        assertTrue("$rule: message '$message' should mention '$mention'", message.contains(mention))
    }

    @Test
    fun `a valid file becomes words in file order with context forms defaulting to the card`() {
        val text = wordsFile(
            word("contextAnswer" to "\"overtaking\""),
            word(
                "id" to "\"get-a-lift\"",
                "english" to "\"get a lift\"",
                "example" to "\"I got a lift home.\"",
                "polish" to "\"zostać podwiezionym\"",
                "contextSentence" to "\"Can I get a lift to the station?\"",
            ),
            word("id" to "\"hedgehog\"", "english" to "\"hedgehog\"", "example" to "\"A hedgehog hid.\"", "polish" to "\"jeż\""),
        )

        val words = parser.parse(VocabFile("02-driving.json", text))

        assertEquals(
            listOf(
                VocabWord("overtake", "overtake", "I waited before overtaking the lorry.", "wyprzedzać", contextAnswer = "overtaking"),
                VocabWord(
                    "get-a-lift", "get a lift", "I got a lift home.", "zostać podwiezionym",
                    contextSentence = "Can I get a lift to the station?",
                ),
                VocabWord("hedgehog", "hedgehog", "A hedgehog hid.", "jeż"),
            ),
            words,
        )
        assertEquals("hedgehog", words[2].contextAnswer)
        assertEquals("A hedgehog hid.", words[2].contextSentence)
    }

    @Test
    fun `a file that breaks one content rule is rejected and the message names the file and the field`() {
        val second = { override: Pair<String, String?> -> wordsFile(word(), word("id" to "\"second\"", override)) }
        val cases = listOf(
            Triple("words is missing", file("words" to null), "words must be a non-empty array"),
            Triple("words is empty", file("words" to "[]"), "words must be a non-empty array"),
            Triple("words is an object", file("words" to "{}"), "words must be a non-empty array"),
            Triple("a word is a string", file("words" to "[\"dump\"]"), "word 1: must be an object"),
            Triple("a word has no id", wordsFile(word("id" to null)), "word 1: id must be"),
            Triple("an id is a number", wordsFile(word("id" to "7")), "word 1: id must be"),
            Triple("english is missing", second("english" to null), "word 2: english must be a non-blank string"),
            Triple("english is blank", second("english" to "\"  \""), "word 2: english must be a non-blank string"),
            Triple("example is null", second("example" to "null"), "word 2: example must be a non-blank string"),
            Triple("polish is a number", second("polish" to "42"), "word 2: polish must be a non-blank string"),
            Triple("contextAnswer is blank", second("contextAnswer" to "\" \""), "word 2: contextAnswer must be"),
            Triple("contextSentence is null", second("contextSentence" to "null"), "word 2: contextSentence must be"),
            Triple("a word field is misspelled", second("contextanswer" to "\"x\""), "word 2: unknown field contextanswer"),
            Triple("a file field is unknown", file("title" to "\"Driving\""), "unknown field title"),
            Triple("the text is not JSON", "this is not json", "not a valid JSON object"),
            Triple("the top level is an array", "[${word()}]", "not a valid JSON object"),
        )

        cases.forEach { (rule, text, mention) -> assertRejected("01-general.json", text, mention, "has this problem: $rule") }
    }

    @Test
    fun `an id that could break the saved progress is rejected`() {
        val ids = listOf(
            "dump,out" to "comma",
            "dump=out" to "equals sign",
            "dump;out" to "semicolon",
            "dump|out" to "pipe",
            "dump out" to "space",
            "Dump-Out" to "upper-case letter",
            "dump_out" to "underscore",
            "-dump" to "leading hyphen",
            "dump--out" to "double hyphen",
            "" to "nothing",
        )

        ids.forEach { (id, what) ->
            assertRejected("01-general.json", wordsFile(word("id" to "\"$id\"")), "id", "uses an id with a $what")
        }
    }

    @Test
    fun `a file name that is not a lower-case slug ending in json is rejected`() {
        val names = listOf("Driving.json", "driving.txt", "driving", "02 driving.json", "02_driving.json", ".json")

        names.forEach { name -> assertRejected(name, file(), "file name must be", "is named '$name'") }
    }

    @Test
    fun `files are combined in file name order whatever order they are listed in`() {
        fun only(id: String) = wordsFile(word("id" to "\"$id\""))

        val words = parser.parseAll(
            listOf(
                VocabFile("10-later.json", only("later")),
                VocabFile("02-driving.json", only("driving")),
                VocabFile("01-general.json", only("general")),
            ),
        )

        assertEquals(listOf("general", "driving", "later"), words.map { it.id })
    }

    @Test
    fun `an id repeated in another file or the same file is rejected with the files that use it`() {
        val acrossFiles = failure {
            parser.parseAll(
                listOf(VocabFile("02-driving.json", file()), VocabFile("01-general.json", file())),
            )
        }
        val withinFile = failure { parser.parseAll(listOf(VocabFile("01-general.json", wordsFile(word(), word())))) }

        assertEquals("id overtake is used more than once (01-general.json, 02-driving.json)", acrossFiles)
        assertEquals("id overtake is used more than once (01-general.json, 01-general.json)", withinFile)
    }

    @Test
    fun `no files give no words`() {
        assertEquals(emptyList<VocabWord>(), parser.parseAll(emptyList()))
    }
}
