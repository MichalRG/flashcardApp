package com.krzyzowski.vocabboost.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Compose end screen has no JVM test infrastructure, so the owner-approved wording (AC14)
 * and the documentation (AC19) are checked as text. Resource names are deliberately not asserted.
 */
class EndScreenContractTest {
    private fun fileFromModuleOrRoot(path: String): String {
        val candidates = listOf(File(path), File("app/$path"), File("../$path"))
        return candidates.first { it.isFile }.readText()
    }

    private val strings get() = fileFromModuleOrRoot("src/main/res/values/strings.xml")
    private val readme get() = (File("README.md").takeIf { it.isFile } ?: File("../README.md")).readText()
        .replace(Regex("\\s+"), " ")

    @Test
    fun `the end screen has the approved completed and not-complete titles`() {
        assertTrue(strings.contains(">Test completed<"))
        assertTrue(strings.contains(">Test ended – not complete<"))
    }

    @Test
    fun `the cards-left text has singular and plural forms with the remaining count`() {
        assertTrue(strings.contains("%1\$d card still left – it will be offered when you start the test again."))
        assertTrue(strings.contains("%1\$d cards still left – they will be offered when you start the test again."))
    }

    @Test
    fun `the old postponement end-screen texts are gone`() {
        assertFalse(strings.contains("test_end_learning_left"))
        assertFalse(strings.contains("test_end_spacing_deferred"))
        assertFalse(strings.contains("was postponed to avoid revealing the answer"))
        assertFalse(strings.contains(">Test finished<"))
    }

    @Test
    fun `explanatory strings no longer promise postponement`() {
        assertFalse(strings.contains("the other direction stays due for another session"))
        assertFalse(strings.contains("Missed cards retry after other cards when there is room"))
    }

    @Test
    fun `the readme no longer documents postponed directions or an unforced retry`() {
        assertFalse(readme.contains("the direction is postponed"))
        assertFalse(readme.contains("does not force an immediate retry"))
        assertFalse(readme.contains("Learning stays unfinished for a later session"))
        assertFalse(readme.contains("including postponed ones"))
        assertFalse(readme.contains("or postpone its opposite"))
    }
}
