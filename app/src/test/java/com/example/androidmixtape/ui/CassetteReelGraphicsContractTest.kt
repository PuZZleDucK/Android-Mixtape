package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CassetteReelGraphicsContractTest {
    @Test
    fun cassetteTapeUsesAnticlockwiseRotationForBothReels() {
        val source = mixtapeAppSource()
        val cassetteTapeSource = source
            .substringAfter("private fun CassetteTape(")
            .substringBefore("private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReel")
        val leftReelUsesAnticlockwiseRotation =
            Regex("""val\s+leftReelRotation\s*=\s*-reelRotation""").containsMatchIn(cassetteTapeSource) &&
                Regex("""rotation\s*=\s*leftReelRotation(?:\s*\*[^,]+)?""").containsMatchIn(cassetteTapeSource)
        val rightReelUsesAnticlockwiseRotation =
            Regex("""val\s+rightReelRotation\s*=\s*-reelRotation""").containsMatchIn(cassetteTapeSource) &&
                Regex("""rotation\s*=\s*rightReelRotation(?:\s*\*[^,]+)?""").containsMatchIn(cassetteTapeSource)

        assertTrue(
            "CassetteTape should keep the existing 16-second linear reel animation loop.",
            cassetteTapeSource.contains("durationMillis = 16_000") && cassetteTapeSource.contains("LinearEasing"),
        )
        assertTrue(
            "Left reel should use negative animated rotation so it appears anticlockwise on Android's clockwise-positive canvas.",
            leftReelUsesAnticlockwiseRotation,
        )
        assertTrue(
            "Right reel should also use negative animated rotation so both cassette reels appear anticlockwise.",
            rightReelUsesAnticlockwiseRotation,
        )
        assertFalse(
            "Right reel must not keep the previous positive rotation; that makes it clockwise/opposite the left reel.",
            Regex("""drawReel\([^\n]*0\.69f[^\n]*rotation\s*=\s*reelRotation""").containsMatchIn(cassetteTapeSource) ||
                (
                    Regex("""val\s+rightReelRotation\s*=\s*reelRotation""").containsMatchIn(cassetteTapeSource) &&
                        Regex("""drawReel\([^\n]*0\.69f[^\n]*rotation\s*=\s*rightReelRotation""").containsMatchIn(cassetteTapeSource)
                    ),
        )
        assertTrue(
            "Existing accessibility semantics for spinning/stopped reels should stay stable.",
            cassetteTapeSource.contains("Cassette reels spinning") && cassetteTapeSource.contains("Cassette reels stopped"),
        )
    }

    @Test
    fun tapeTransportUsesChangingPackRadiiTrueTangentsAndGuideCircumferences() {
        val source = mixtapeAppSource()
        val transportSource = source
            .substringAfter("private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTapeTransport")
            .substringBefore("private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReel")

        assertTrue("Tape pack radii should conserve tape area as playback progresses.", transportSource.contains("tapeArea") && transportSource.contains("sqrt"))
        assertTrue("Both reel packs should change from playback progress.", transportSource.contains("leftRadius") && transportSource.contains("rightRadius") && transportSource.contains("clampedProgress"))
        assertTrue("Tape runs should use common external circle tangents.", source.contains("externalCircleTangent") && transportSource.contains("leftTangent") && transportSource.contains("rightTangent"))
        assertTrue("Tape should wrap around guide-pin circumferences before the lower run.", transportSource.contains("guideRadius") && transportSource.contains("arcTo") && transportSource.contains("lineTo(rightGuide.x, rightGuide.y + guideRadius)"))
    }

    @Test
    fun drawReelUsesChunkyNearBlackEmbossedSpokes() {
        val drawReelSource = drawReelSource()

        assertTrue(
            "drawReel must continue to wrap the spoke artwork in rotate(degrees = rotation, pivot = center).",
            Regex("""rotate\(\s*degrees\s*=\s*rotation\s*,\s*pivot\s*=\s*center""").containsMatchIn(drawReelSource),
        )
        assertTrue(
            "Reel spokes should be materially thicker than the old 5f lines; use a named spoke stroke or direct 8f-10f stroke width.",
            Regex("""(?:spokeStrokeWidth|mainSpokeStrokeWidth)\s*=\s*(?:8|9|10)(?:\.0)?f|strokeWidth\s*=\s*(?:8|9|10)(?:\.0)?f""").containsMatchIn(drawReelSource),
        )
        assertTrue(
            "The main spoke color should be black/near-black, not the old warm brown spoke color.",
            listOf("Color.Black", "0xFF000000", "0xFF11100F", "0xFF151312", "0xFF0F0E0D").any { drawReelSource.contains(it) },
        )
        assertFalse(
            "The old thin brown spoke line must not remain as the only/main spoke treatment.",
            Regex("""drawLine\(\s*color\s*=\s*Color\(0xFF2B2522\)[\s\S]*?strokeWidth\s*=\s*5f""").containsMatchIn(drawReelSource),
        )
        val drawLineCount = Regex("""drawLine\(""").findAll(drawReelSource).count()
        assertTrue(
            "Subtle emboss should be represented by layered spoke lines and/or explicit shadow/highlight/emboss code.",
            drawLineCount >= 3 || Regex("""emboss|shadow|highlight""", RegexOption.IGNORE_CASE).containsMatchIn(drawReelSource),
        )
    }

    @Test
    fun drawReelDrawsRetroBeigeSpindleAboveSpokesWithRotatingAsymmetricDetail() {
        val drawReelSource = drawReelSource()
        val rotateIndex = drawReelSource.indexOf("rotate(degrees = rotation")
        assertTrue("Expected drawReel to contain the rotating reel artwork block.", rotateIndex >= 0)

        assertTrue(
            "Center spindle/hub code should be named clearly so future maintainers keep the retro beige spindle intent.",
            Regex("""spindle|hub""", RegexOption.IGNORE_CASE).containsMatchIn(drawReelSource),
        )
        assertTrue(
            "The spindle should use a warm retro beige/tan/cream palette, not a flat gray/white center only.",
            listOf(
                "beige", "cream", "tan",
                "0xFFE8D9B7", "0xFFF6EACF", "0xFFEAD7B0", "0xFFD7BE86",
                "0xFFC79B61", "0xFFB38A58", "0xFF8B633F",
            ).count { drawReelSource.contains(it, ignoreCase = true) } >= 2,
        )
        assertTrue(
            "The beige spindle disk/rim should be drawn after the spoke layer so it sits visually above the black spokes.",
            Regex("""drawCircle\([\s\S]*(?:spindle|hub|beige|cream|tan|0xFFE8D9B7|0xFFF6EACF|0xFFEAD7B0|0xFFD7BE86|0xFFC79B61|0xFFB38A58)""", RegexOption.IGNORE_CASE)
                .findAll(drawReelSource)
                .any { it.range.first > rotateIndex },
        )
        val rotatingArtwork = drawReelSource.substring(rotateIndex)
        assertTrue(
            "An asymmetric spindle detail such as a slot/notch/groove/dot should be inside the rotation block so the beige center visibly rotates with the reel.",
            Regex("""slot|notch|groove|dot|asymmetric""", RegexOption.IGNORE_CASE).containsMatchIn(rotatingArtwork) &&
                Regex("""draw(?:Line|Circle|RoundRect|Arc)\(""").containsMatchIn(rotatingArtwork),
        )
    }

    private fun drawReelSource(): String {
        val source = mixtapeAppSource()
        val startMarker = "private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReel"
        val endMarker = "@Composable\nprivate fun WalkmanTransportControls"
        assertTrue("Expected to find drawReel in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find WalkmanTransportControls after drawReel in MixtapeApp.kt", source.contains(endMarker))
        return source.substringAfter(startMarker).substringBefore(endMarker).normalizedLineEndings()
    }

    private fun mixtapeAppSource(): String {
        val path = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("app/src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find MixtapeApp.kt from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText().normalizedLineEndings()
    }

    private fun String.normalizedLineEndings(): String = replace("\r\n", "\n")
}
