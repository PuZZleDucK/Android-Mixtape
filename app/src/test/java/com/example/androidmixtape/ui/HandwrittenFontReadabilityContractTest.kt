package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwrittenFontReadabilityContractTest {
    @Test
    fun cassetteHandwritingUsesSelectedMixtapeFontInsteadOfGenericCursive() {
        val source = mixtapeAppSource()

        assertFalse(
            "Cassette handwriting must use the selected per-mixtape font family, not generic FontFamily.Cursive.",
            source.contains("FontFamily.Cursive"),
        )

        listOf("CassetteSpineRow", "CassetteTape", "CassetteCoverTrackList").forEach { functionName ->
            val functionSource = extractFunction(source, functionName)
            assertTrue(
                "$functionName must accept/use the selected handwritingFont so spine, tape title, and track list stay consistent.",
                functionSource.contains("handwritingFont"),
            )
        }
    }

    @Test
    fun cassetteHandwritingStillDeclaresReadableEffectiveWeights() {
        val source = mixtapeAppSource()
        val handwrittenTextCalls = extractTextCalls(source)
            .filter { it.contains("handwritingFont") || it.contains("cassetteHandwriting") || it.contains("effectiveCassetteWeight") }

        assertTrue(
            "Expected cassette handwriting Text calls to use the selected handwriting font mapping.",
            handwrittenTextCalls.isNotEmpty(),
        )

        val missingExplicitWeight = handwrittenTextCalls.filterNot {
            Regex("""fontWeight\s*=""").containsMatchIn(it)
        }
        val weakExplicitWeight = handwrittenTextCalls.filter {
            Regex("""FontWeight\.(Normal|Light|Thin|ExtraLight|W100|W200|W300|W400)\b""").containsMatchIn(it)
        }

        assertTrue(
            "Every cassette handwriting Text call must explicitly set fontWeight, preferably through effectiveCassetteWeight(...). " +
                "Missing fontWeight in:\n${missingExplicitWeight.joinToString("\n\n") { it.readableSnippet() }}",
            missingExplicitWeight.isEmpty(),
        )
        assertTrue(
            "Cassette handwriting Text must not use Normal/Light weights directly; Indie Flower and Shadows Into Light need bold handling. " +
                "Weak weights in:\n${weakExplicitWeight.joinToString("\n\n") { it.readableSnippet() }}",
            weakExplicitWeight.isEmpty(),
        )
    }

    private fun extractTextCalls(source: String): List<String> = extractCalls(source, "Text")

    private fun extractFunction(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName")
        assertTrue("Expected to find function $functionName in MixtapeApp.kt", start >= 0)
        val nextFunction = source.indexOf("\nprivate fun ", start + 1).takeIf { it >= 0 } ?: source.length
        return source.substring(start, nextFunction)
    }

    private fun extractCalls(source: String, callName: String): List<String> {
        val calls = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val start = source.indexOf("$callName(", searchFrom)
            if (start == -1) return calls

            var index = start + callName.length
            var depth = 0
            while (index < source.length) {
                when (source[index]) {
                    '(' -> depth += 1
                    ')' -> {
                        depth -= 1
                        if (depth == 0) {
                            calls += source.substring(start, index + 1)
                            searchFrom = index + 1
                            break
                        }
                    }
                }
                index += 1
            }

            if (index >= source.length) return calls
        }
    }

    private fun String.readableSnippet(): String =
        lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(9)
            .joinToString("\n")

    private fun mixtapeAppSource(): String {
        val path = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("app/src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find MixtapeApp.kt from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText()
    }
}
