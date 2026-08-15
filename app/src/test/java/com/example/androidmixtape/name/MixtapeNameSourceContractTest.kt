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
}
