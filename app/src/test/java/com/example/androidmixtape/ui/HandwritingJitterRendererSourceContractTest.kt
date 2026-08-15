package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingJitterRendererSourceContractTest {
    @Test
    fun jitterRendererUsesCanvasTextAndExposesComposeTextSemantics() {
        val source = jitterRendererSource()

        assertTrue("JitteredHandwritingText composable must exist as the cassette handwriting renderer layer.", source.contains("fun JitteredHandwritingText"))
        assertTrue("Renderer should use Compose Canvas so individual character/word tokens can be offset/rotated.", source.contains("Canvas("))
        assertTrue("Renderer should use rememberTextMeasurer for Canvas text layout.", source.contains("rememberTextMeasurer"))
        assertTrue("Renderer should draw measured text on Canvas rather than delegating to one plain TextView/Text call.", source.contains("drawText"))
        assertTrue(
            "Renderer must leave punctuation, spaces, and emoji unmodified.",
            source.contains("handwritingTokenAcceptsPerturbation(token)"),
        )
        assertTrue(
            "Canvas-rendered handwriting must expose Compose text semantics so onNodeWithText(...) and accessibility text still find the original string; contentDescription alone is insufficient.",
            rendererExposesComposeTextSemantics(source),
        )
        assertFalse(
            "JitteredHandwritingText must not use contentDescription = text as the text-semantics substitute because it overwrites/conflicts with caller content descriptions such as cassette row labels.",
            Regex("contentDescription\\s*=\\s*text").containsMatchIn(source),
        )
    }

    @Test
    fun jitterRendererDoesNotIntroduceDuplicateVisibleTextNodes() {
        val source = jitterRendererSource()
        val textCall = Regex("\\bText\\s*\\(").containsMatchIn(source)

        if (textCall) {
            assertTrue(
                "If JitteredHandwritingText uses a fallback Text carrier, that Text must be explicitly semantics-only/transparent/invisible so the Canvas remains the only visible handwriting layer.",
                source.contains("Color.Transparent") ||
                    source.contains("alpha(0f)") ||
                    source.contains("clearAndSetSemantics") ||
                    source.contains("invisibleToUser"),
            )
        }
        assertFalse(
            "Do not preserve semantics by drawing a second visible Text with the same string; that would duplicate onNodeWithText(...) matches and make cassette labels look double-rendered.",
            Regex("\\bText\\s*\\([^)]*(color\\s*=\\s*color|style\\s*=\\s*style|fontSize\\s*=\\s*fontSize)", RegexOption.DOT_MATCHES_ALL).containsMatchIn(source),
        )
    }

    @Test
    fun jitterRendererSkipsDrawingWhenCanvasWidthIsCollapsed() {
        val source = jitterRendererSource()

        assertTrue(
            "JitteredHandwritingText must guard a zero/near-zero Canvas width and skip visual drawing instead of rendering only the first token fragment while semantics still expose the full text.",
            rendererGuardsCollapsedCanvasWidth(source),
        )
    }

    @Test
    fun jitterGenerationDoesNotUseWallClockOrUnseededRandom() {
        val jitterSources = uiSourceFiles().filter { it.name.contains("Jitter", ignoreCase = true) }
        val source = jitterSources.joinToString("\n") { it.readText() }

        assertTrue("Expected at least one Jitter source file under modern UI.", jitterSources.isNotEmpty())
        listOf(
            "Random.Default",
            "System.currentTimeMillis",
            "System.nanoTime",
            "Clock.System",
            "Instant.now",
            "Date()",
        ).forEach { forbidden ->
            assertFalse("Jitter generation must be seeded only from stable mixtape/surface/text material, not $forbidden.", source.contains(forbidden))
        }
    }

    @Test
    fun cassetteHandwrittenJitterCallSitesAllocateRealWidth() {
        val source = mixtapeAppSource()

        val calls = allJitteredCalls(source)
        assertTrue("Expected all cassette handwriting surfaces to use the shared renderer.", calls.size >= 7)
        calls.forEachIndexed { index, call ->
            assertTrue(
                "JitteredHandwritingText call $index must allocate real width so its characters remain legible.",
                jitteredCallAllocatesWidth(call),
            )
        }
    }

    @Test
    fun cassetteHandwrittenSurfacesAreRenderedThroughJitteredHandwritingText() {
        val source = mixtapeAppSource()

        assertAtLeastJitteredCalls(source, "CassetteSpineRow", expectedMinimum = 1)
        assertAtLeastJitteredCalls(source, "CassetteTape", expectedMinimum = 1)
        assertAtLeastJitteredCalls(source, "CassetteCoverTrackList", expectedMinimum = 3)

        assertFalse("Renderer calls should no longer hash text into fresh random sequences.", source.contains("seedMaterial ="))
        assertTrue("Mixtape names should begin at the persisted start index.", source.contains("startIndex = mixtapeJitterStartIndex"))
        assertTrue("Track rows should use deterministic 100-entry offsets.", source.contains("handwritingTrackStartIndex(mixtapeJitterStartIndex, index)"))
    }

    private fun rendererExposesComposeTextSemantics(source: String): Boolean {
        val explicitTextSemantics = listOf(
            "this.text = AnnotatedString(text)",
            "text = AnnotatedString(text)",
            "text = listOf(AnnotatedString(text))",
            "SemanticsProperties.Text",
            "SemanticsPropertyReceiver.text",
        ).any { source.contains(it) }
        val semanticsCarrierText = Regex("\\bText\\s*\\([^)]*text\\s*=\\s*text", RegexOption.DOT_MATCHES_ALL).containsMatchIn(source) &&
            (source.contains("Color.Transparent") || source.contains("alpha(0f)") || source.contains("clearAndSetSemantics") || source.contains("invisibleToUser"))
        return explicitTextSemantics || semanticsCarrierText
    }

    private fun rendererGuardsCollapsedCanvasWidth(source: String): Boolean {
        val collapsedWidthCheck = listOf(
            "size.width <= 0f",
            "size.width < 1f",
            "size.width <= 1f",
            "size.width <= minimumDrawableWidth",
            "size.width < minimumDrawableWidth",
        ).any { source.contains(it) }
        return collapsedWidthCheck && source.contains("return@Canvas")
    }

    private fun allJitteredCalls(source: String): List<String> =
        Regex("\\bJitteredHandwritingText\\s*\\(")
            .findAll(source)
            .map { match -> extractBalancedCall(source, match.range.first) }
            .toList()

    private fun extractBalancedCall(source: String, callStart: Int): String {
        val open = source.indexOf('(', callStart)
        assertTrue("Expected call opening parenthesis.", open >= 0)
        var depth = 0
        for (index in open until source.length) {
            when (source[index]) {
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(callStart, index + 1)
                }
            }
        }
        throw AssertionError("Could not extract balanced JitteredHandwritingText call.")
    }

    private fun jitteredCallAllocatesWidth(call: String): Boolean = Regex(
        "modifier\\s*=\\s*Modifier[\\s\\S]*(\\.fillMaxWidth\\s*\\(|\\.weight\\s*\\(|\\.width\\s*\\(|\\.widthIn\\s*\\()",
    ).containsMatchIn(call)

    private fun assertAtLeastJitteredCalls(source: String, functionName: String, expectedMinimum: Int) {
        val functionSource = extractFunction(source, functionName)
        val callCount = Regex("\\bJitteredHandwritingText\\s*\\(").findAll(functionSource).count()
        assertTrue(
            "$functionName should use JitteredHandwritingText for its handwritten cassette labels; found $callCount calls.",
            callCount >= expectedMinimum,
        )
    }

    private fun extractFunction(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName")
        assertTrue("Expected to find function $functionName in MixtapeApp.kt", start >= 0)
        val nextFunction = source.indexOf("\nprivate fun ", start + 1).takeIf { it >= 0 } ?: source.length
        return source.substring(start, nextFunction)
    }

    private fun jitterRendererSource(): String = uiSourceFiles()
        .firstOrNull { it.readText().contains("fun JitteredHandwritingText") }
        ?.readText()
        ?: ""

    private fun uiSourceFiles(): List<File> {
        val uiDir = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui"),
            File("src/modern/java/com/example/androidmixtape/ui"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected modern UI source directory from ${System.getProperty("user.dir")}", uiDir != null)
        return uiDir!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun mixtapeAppSource(): String {
        val path = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find MixtapeApp.kt from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText()
    }
}
