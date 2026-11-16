package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedAnswerTest {
    private val word = VocabWord("dump", "dump", "We took the broken furniture to the dump.", "wysypisko śmieci")

    /** An ENGLISH_FIRST card expects Polish text. */
    private fun polish(expected: String, typed: String) =
        TypedAnswer.isCorrect(typed, expected, CardDirection.ENGLISH_FIRST)

    /** A POLISH_FIRST card expects English text. */
    private fun english(expected: String, typed: String) =
        TypedAnswer.isCorrect(typed, expected, CardDirection.POLISH_FIRST)

    private fun assertAccepted(check: (String, String) -> Boolean, expected: String, vararg inputs: String) =
        inputs.forEach { assertTrue("'$it' should match '$expected'", check(expected, it)) }

    private fun assertRejected(check: (String, String) -> Boolean, expected: String, vararg inputs: String) =
        inputs.forEach { assertFalse("'$it' should not match '$expected'", check(expected, it)) }

    @Test
    fun `an English first card expects the Polish text`() {
        assertEquals("wysypisko śmieci", CardDirection.ENGLISH_FIRST.expected(word))
    }

    @Test
    fun `a Polish first card expects the English word`() {
        assertEquals("dump", CardDirection.POLISH_FIRST.expected(word))
    }

    @Test
    fun `normalization drops parentheticals folds case and diacritics and tidies whitespace`() {
        assertEquals("szczupak", TypedAnswer.normalize("szczupak (ryba)"))
        assertEquals("clutch pedal", TypedAnswer.normalize("Clutch (car)   pedal"))
        assertEquals("zaczynac lodz zolc", TypedAnswer.normalize("  Zaczynać   ŁÓDŹ\tŻółć "))
        assertEquals("a c e l n o s z z", TypedAnswer.normalize("ą ć ę ł ń ó ś ź ż"))
        assertEquals("a c e l n o s z z", TypedAnswer.normalize("Ą Ć Ę Ł Ń Ó Ś Ź Ż"))
    }

    @Test
    fun `normalization strips a trailing run of dots exclamation and question marks only`() {
        assertEquals("dump", TypedAnswer.normalize("dump."))
        assertEquals("dump", TypedAnswer.normalize("Dump?!"))
        assertEquals("dump", TypedAnswer.normalize("dump..."))
        assertEquals("a.b", TypedAnswer.normalize("a.b"))
    }

    @Test
    fun `normalization leaves hyphens and apostrophes alone`() {
        assertEquals("full-time", TypedAnswer.normalize("Full-Time"))
        assertEquals("don't", TypedAnswer.normalize("Don't"))
    }

    @Test
    fun `parentheticals are removed before splitting so their separators do not create alternatives`() {
        val expected = "zrobiłbym, zrobiłby (np. w sytuacji hipotetycznej; nie oznacza 100% pewności)"

        assertEquals(setOf("zrobilbym", "zrobilby"), TypedAnswer.alternatives(expected, CardDirection.ENGLISH_FIRST).toSet())
    }

    @Test
    fun `Polish expected text splits on comma semicolon and slash`() {
        assertEquals(
            setOf("a", "b", "c", "d"),
            TypedAnswer.alternatives("a, b; c / d", CardDirection.ENGLISH_FIRST).toSet(),
        )
        assertEquals(
            setOf("pelnoetatowy", "na pelen etat"),
            TypedAnswer.alternatives("na pełen etat; pełnoetatowy", CardDirection.ENGLISH_FIRST).toSet(),
        )
    }

    @Test
    fun `English expected text splits on semicolon and slash but not on comma`() {
        assertEquals(
            setOf("a, b", "c", "d"),
            TypedAnswer.alternatives("a, b; c / d", CardDirection.POLISH_FIRST).toSet(),
        )
        assertEquals(setOf("no pain, no gain"), TypedAnswer.alternatives("no pain, no gain", CardDirection.POLISH_FIRST).toSet())
    }

    @Test
    fun `empty parts produced by splitting are discarded`() {
        assertEquals(setOf("a", "b"), TypedAnswer.alternatives(" ,a,, b ; ", CardDirection.ENGLISH_FIRST).toSet())
        assertEquals(setOf("a"), TypedAnswer.alternatives("a / / (x)", CardDirection.POLISH_FIRST).toSet())
    }

    @Test
    fun `a Polish entry with several meanings accepts one alternative or any subset`() {
        val expected = "wysypać, wylać, opróżnić"

        assertAccepted(::polish, expected, "wylać", "WYLAC", "oproznic", "wylać, opróżnić", "wysypać, wylać, opróżnić", "wylać.")
    }

    @Test
    fun `a Polish entry with several meanings rejects partial or foreign answers`() {
        val expected = "wysypać, wylać, opróżnić"

        assertRejected(::polish, expected, "wyla", "wylać i opróżnić", "wylać, pić", "", "wysypac wylac oproznic")
    }

    @Test
    fun `duplicate typed parts and empty typed parts are tolerated`() {
        val expected = "wysypać, wylać, opróżnić"

        assertAccepted(::polish, expected, "wylać, wylać", "wylać,,opróżnić", ",wylać,")
    }

    @Test
    fun `parenthetical notes are ignored on both the expected and the typed side`() {
        assertAccepted(::polish, "szczupak (ryba)", "szczupak", "szczupak (ryba)", "Szczupak (cokolwiek)")
        assertRejected(::polish, "szczupak (ryba)", "ryba", "(ryba)", "szczupak ryba")
        assertAccepted(::english, "clutch (car)", "clutch", "clutch (car)")
    }

    @Test
    fun `missing Polish diacritics and letter case are accepted`() {
        assertAccepted(::polish, "ryś", "rys", "Ryś!", "RYŚ", "RYS")
        assertAccepted(::polish, "łódź", "lodz", "ŁÓDŹ", "Łódź")
        assertAccepted(::polish, "Żółć", "zolc", "ŻÓŁĆ")
        assertAccepted(::polish, "ąęśćźżńół", "aesczznol")
    }

    @Test
    fun `semicolon separated Polish alternatives are each accepted`() {
        assertAccepted(::polish, "na pełen etat; pełnoetatowy", "pelnoetatowy", "na pelen etat")
        assertRejected(::polish, "na pełen etat; pełnoetatowy", "na pelen")
    }

    @Test
    fun `an np clause counts as an alternative when typed literally`() {
        val expected = "wpłynąć na plan, np. wymusić jego zmianę"

        assertAccepted(::polish, expected, "wplynac na plan", "np. wymusić jego zmianę")
        assertRejected(::polish, expected, "wymusic jego zmiane")
    }

    @Test
    fun `a Polish slash separates alternatives`() {
        assertAccepted(::polish, "śruba / śruby", "sruby", "sruba", "sruba/sruby", "sruba, sruby", "śruba / śruby")
    }

    @Test
    fun `an English entry with slash alternatives accepts either or both`() {
        val expected = "flat tyre / flat tire"

        assertAccepted(::english, expected, "flat tire", "Flat Tyre", "flat tyre/flat tire", "flat tyre / flat tire", "flat tire?")
        assertRejected(::english, expected, "flat", "flat tyres", "flat tir")
    }

    @Test
    fun `an English comma is not a separator`() {
        val expected = "no pain, no gain"

        assertAccepted(::english, expected, "no pain, no gain", "No pain,  no gain!")
        assertRejected(::english, expected, "no pain", "no gain")
        assertRejected(::english, "bolt / bolts", "bolt, bolts")
        assertAccepted(::english, "bolt / bolts", "bolts", "bolt", "bolt / bolts")
    }

    @Test
    fun `a Polish comma list is not split when the card expects English`() {
        assertRejected(::english, "wylać, opróżnić", "wylać")
    }

    @Test
    fun `an English semicolon separates alternatives`() {
        assertAccepted(::english, "foo; bar", "foo", "bar", "bar; foo")
    }

    @Test
    fun `hyphens are not normalized`() {
        assertAccepted(::english, "full-time", "full-time", "Full-Time")
        assertRejected(::english, "full-time", "full time", "fulltime")
    }

    @Test
    fun `apostrophes are not normalized`() {
        assertRejected(::english, "don't", "dont")
        assertAccepted(::english, "don't", "Don't")
    }

    @Test
    fun `trailing dots exclamation and question marks are ignored on typed and expected text`() {
        assertAccepted(::english, "dump", "dump.", "dump!", "dump?", "dump?!")
        assertAccepted(::english, "etc.", "etc", "etc.")
        assertRejected(::english, "dump", "dum.p")
    }

    @Test
    fun `blank and whitespace only input is never correct`() {
        assertRejected(::polish, "ryś", "", " ", "   ", "\t", "\n")
        assertRejected(::english, "dump", "", "  ")
    }

    @Test
    fun `input that is empty after normalization is wrong`() {
        assertRejected(::polish, "szczupak (ryba)", "(ryba)")
        assertRejected(::polish, "ryś", "...", "?!", "(x)")
        assertRejected(::english, "dump", "...", "(dump)")
    }

    @Test
    fun `typos are not tolerated`() {
        assertAccepted(::polish, "wysypać, wylać, opróżnić", "wylac")
        assertRejected(::polish, "wysypać, wylać, opróżnić", "wylaćc", "wylc", "wylatc")
        assertRejected(::english, "clutch (car)", "clutc", "clutchh")
        assertRejected(::english, "flat tyre / flat tire", "flat tyer", "flat tire s")
    }

    @Test
    fun `matching does not depend on the order or repeats of typed parts`() {
        assertAccepted(::polish, "wysypać, wylać, opróżnić", "opróżnić, wysypać", "opróżnić; wylać", "wylać / opróżnić")
    }
}
