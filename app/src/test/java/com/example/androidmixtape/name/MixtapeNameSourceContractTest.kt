package com.example.androidmixtape.name

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MixtapeNameSourceContractTest {
    private val namesFile = File("src/main/assets/mixtape_names.txt")

    @Test
    fun bundledNameListContainsEveryUniqueNonBlankName() {
        assertTrue("Expected the bundled mixtape name list at ${namesFile.path}.", namesFile.isFile)
        val names = namesFile.readLines()

        assertEquals(1_714, names.size)
        assertTrue(names.all { it.isNotBlank() })
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun bundledNamesReadLikeTitlesRatherThanGeneratorFragments() {
        val names = namesFile.readLines()
        val allowedCharacters = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N} '&,!?:/.-]*")
        val throwawayTitles = setOf("DIY", "RE:", "11AM ++", "~~~", "etc.", "maybe", "7AM")

        assertTrue("Replace generator fragments: ${names.filter { it in throwawayTitles }.take(12)}",
            names.none { it in throwawayTitles })
        assertTrue("Replace titles made of emoji or punctuation: ${names.filterNot { allowedCharacters.matches(it) }.take(12)}",
            names.all { allowedCharacters.matches(it) })
        assertTrue("Keep titles within one to four words: ${names.filter { it.trim().split(Regex("\\s+")).size !in 1..4 }.take(12)}",
            names.all { it.trim().split(Regex("\\s+")).size in 1..4 })
        assertTrue("Remove filler suffixes: ${names.filter { Regex("(?i)\\b(?:etc|maybe|online|offline|v2)\\.?$|(?:[!?./_+~;*]){2,}$").containsMatchIn(it) }.take(12)}",
            names.none { Regex("(?i)\\b(?:etc|maybe|online|offline|v2)\\.?$|(?:[!?./_+~;*]){2,}$").containsMatchIn(it) })
    }

    @Test
    fun bundledNamesHaveNoCaseOnlyDuplicates() {
        val names = namesFile.readLines()
        assertEquals(names.size, names.map { it.lowercase() }.distinct().size)
    }
}
