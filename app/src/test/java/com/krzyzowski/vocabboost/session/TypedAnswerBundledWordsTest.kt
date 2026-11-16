package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.BUNDLED_WORDS
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedAnswerBundledWordsTest {
    @Test
    fun `bundled vocabulary is loaded for the self consistency checks`() {
        assertTrue(BUNDLED_WORDS.isNotEmpty())
    }

    @Test
    fun `every bundled word accepts its own expected text in both directions`() {
        val failures = BUNDLED_WORDS.flatMap { word ->
            CardDirection.entries.mapNotNull { direction ->
                val expected = direction.expected(word)
                "${word.id} $direction '$expected'".takeUnless { TypedAnswer.isCorrect(expected, expected, direction) }
            }
        }

        assertTrue("Own expected text rejected: $failures", failures.isEmpty())
    }

    @Test
    fun `every individual alternative of every bundled word is accepted`() {
        val failures = BUNDLED_WORDS.flatMap { word ->
            CardDirection.entries.flatMap { direction ->
                val expected = direction.expected(word)
                TypedAnswer.alternatives(expected, direction).mapNotNull { alternative ->
                    "${word.id} $direction '$alternative' of '$expected'"
                        .takeUnless { TypedAnswer.isCorrect(alternative, expected, direction) }
                }
            }
        }

        assertTrue("Alternatives rejected: $failures", failures.isEmpty())
    }

    @Test
    fun `no bundled expected text normalizes to an empty alternative set`() {
        val empty = BUNDLED_WORDS.flatMap { word ->
            CardDirection.entries.mapNotNull { direction ->
                val expected = direction.expected(word)
                "${word.id} $direction '$expected'".takeIf { TypedAnswer.alternatives(expected, direction).isEmpty() }
            }
        }

        assertTrue("Empty alternative sets: $empty", empty.isEmpty())
    }

    @Test
    fun `an empty answer is rejected for every bundled word`() {
        val accepted = BUNDLED_WORDS.flatMap { word ->
            CardDirection.entries.mapNotNull { direction ->
                "${word.id} $direction".takeIf { TypedAnswer.isCorrect("  ", direction.expected(word), direction) }
            }
        }

        assertTrue("Blank accepted: $accepted", accepted.isEmpty())
    }
}
