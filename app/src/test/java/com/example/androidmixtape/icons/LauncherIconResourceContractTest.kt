package com.example.androidmixtape.icons

import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.InflaterInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconResourceContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))

    private val legacyIconSizes = linkedMapOf(
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192,
    )

    private val adaptiveForegroundSizes = linkedMapOf(
        "mdpi" to 108,
        "hdpi" to 162,
        "xhdpi" to 216,
        "xxhdpi" to 324,
        "xxxhdpi" to 432,
    )

    @Test
    fun approvedCassetteLogoMasterIsKeptInRepo() {
        val master = File(projectDir, "app/icon-source/ic_launcher_cassette_1024.png")

        assertTrue(
            "Keep the approved card #2193 1024x1024 cassette launcher logo in the repo for future regeneration. Expected: ${master.relativeTo(projectDir)}",
            master.isFile,
        )

        val size = readPngSize(master)
        assertEquals("Master launcher icon width should remain the approved 1024px source size.", 1024, size.width)
        assertEquals("Master launcher icon height should remain the approved 1024px source size.", 1024, size.height)
    }

    @Test
    fun freshTransparentLogoSourceExistsBeforeLauncherExportsAreRegenerated() {
        val transparentLogo = File(projectDir, "app/icon-source/android_mixtape_logo_transparent_1024.png")

        assertTrue(
            "Card #2212 rework should add a freshly generated transparent cassette logo source before exporting launcher resources. Expected: ${transparentLogo.relativeTo(projectDir)}",
            transparentLogo.isFile,
        )

        val size = readPngSize(transparentLogo)
        assertEquals("Fresh transparent logo source should use the 1024px launcher source canvas width.", 1024, size.width)
        assertEquals("Fresh transparent logo source should use the 1024px launcher source canvas height.", 1024, size.height)

        val image = readPngImage(transparentLogo)
        assertTrue(
            "Fresh transparent logo source must be stored with alpha, not as an opaque RGB square.",
            image.hasAlphaChannel,
        )
        assertTrue(
            "Fresh transparent logo source must have transparent corners so it reviews as logo art on a checkerboard.",
            image.cornerAlphas().all { alpha -> alpha == 0 },
        )
        assertVisibleArtworkWidth(
            file = transparentLogo,
            minWidthRatio = 0.80,
            maxWidthRatio = 0.92,
            reason = "The fresh generated logo should make the cassette dominant while retaining transparent padding for launcher masks.",
        )

        val archivedOriginal = File(projectDir, "app/icon-source/archive/ic_launcher_cassette_1024_card2212_original.png")
        if (archivedOriginal.isFile) {
            assertTrue(
                "Fresh transparent logo source should not be a byte-for-byte copy of the archived opaque square source.",
                !transparentLogo.readBytes().contentEquals(archivedOriginal.readBytes()),
            )
        }
    }

    @Test
    fun launcherIconGeneratorUsesFreshLogoSourceInsteadOfMaskingTheOldOpaqueTile() {
        val generator = File(projectDir, "scripts/generate_launcher_icons.rb")
        val script = generator.readText()

        assertTrue(
            "Icon generation should consume the fresh transparent source art so launcher exports are reproducible from the new logo.",
            script.contains("android_mixtape_logo_transparent_1024.png"),
        )
        assertTrue(
            "Icon generation should no longer use the archived opaque card #2212 square as its primary source.",
            !script.contains("ic_launcher_cassette_1024_card2212_original.png"),
        )
        assertTrue(
            "Icon generation should not preserve the previous mask-polygon workflow; this rework is for newly generated transparent logo art.",
            !script.contains("CASSETTE_MASK_POLYGON") && !script.contains("CopyOpacity"),
        )
    }

    @Test
    fun launcherIconPngsKeepTransparentBackgroundsInsteadOfOpaqueTiles() {
        val launcherPngs = listOf(File(projectDir, "app/icon-source/ic_launcher_cassette_1024.png")) +
            legacyIconSizes.keys.map { density -> File(projectDir, "app/src/main/res/mipmap-$density/ic_launcher.png") } +
            adaptiveForegroundSizes.keys.map { density -> File(projectDir, "app/src/main/res/mipmap-$density/ic_launcher_foreground.png") }

        launcherPngs.forEach { icon ->
            val image = readPngImage(icon)
            val relativePath = icon.relativeTo(projectDir)

            assertTrue(
                "$relativePath must be stored as an RGBA/grayscale-alpha PNG so empty launcher background area can remain transparent.",
                image.hasAlphaChannel,
            )
            assertTrue(
                "$relativePath must have transparent corners; an opaque square/dark tile behind the cassette is not acceptable.",
                image.cornerAlphas().all { alpha -> alpha == 0 },
            )
        }
    }

    @Test
    fun cassetteArtworkFillsMoreOfLauncherCanvasWithoutClippingEdges() {
        val master = File(projectDir, "app/icon-source/ic_launcher_cassette_1024.png")
        assertVisibleArtworkWidth(
            file = master,
            minWidthRatio = 0.80,
            maxWidthRatio = 0.92,
            reason = "The regenerated 1024px source should make the cassette visually dominant while keeping transparent edge padding.",
        )

        legacyIconSizes.keys.forEach { density ->
            assertVisibleArtworkWidth(
                file = File(projectDir, "app/src/main/res/mipmap-$density/ic_launcher.png"),
                minWidthRatio = 0.80,
                maxWidthRatio = 0.92,
                reason = "$density legacy bitmap fallback should scale the cassette up to roughly 82-90% of the icon width.",
            )
        }

        adaptiveForegroundSizes.keys.forEach { density ->
            assertVisibleArtworkWidth(
                file = File(projectDir, "app/src/main/res/mipmap-$density/ic_launcher_foreground.png"),
                minWidthRatio = 0.78,
                maxWidthRatio = 0.92,
                reason = "$density adaptive foreground should be larger than the old 2/3-width safe-zone rendering but still leave transparent padding.",
            )
        }
    }

    @Test
    fun legacyBitmapLauncherIconsCoverAndroidDensitySizes() {
        legacyIconSizes.forEach { (density, sizePx) ->
            val icon = File(projectDir, "app/src/main/res/mipmap-$density/ic_launcher.png")
            assertTrue(
                "API 19 legacy builds need a bitmap @mipmap/ic_launcher for $density. Expected: ${icon.relativeTo(projectDir)}",
                icon.isFile,
            )

            val size = readPngSize(icon)
            assertEquals("$density ic_launcher.png width should be the standard Android launcher size.", sizePx, size.width)
            assertEquals("$density ic_launcher.png height should be the standard Android launcher size.", sizePx, size.height)
        }
    }

    @Test
    fun adaptiveForegroundLauncherIconsCoverAndroidDensitySizes() {
        adaptiveForegroundSizes.forEach { (density, sizePx) ->
            val icon = File(projectDir, "app/src/main/res/mipmap-$density/ic_launcher_foreground.png")
            assertTrue(
                "Android 8+ adaptive launcher icons need a foreground bitmap for $density. Expected: ${icon.relativeTo(projectDir)}",
                icon.isFile,
            )

            val size = readPngSize(icon)
            assertEquals("$density ic_launcher_foreground.png width should match the adaptive foreground canvas size.", sizePx, size.width)
            assertEquals("$density ic_launcher_foreground.png height should match the adaptive foreground canvas size.", sizePx, size.height)
        }
    }

    @Test
    fun manifestLauncherIconRefsHaveAdaptiveIconResourcesForApi26Plus() {
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml").readText()
        val launcherRefs = Regex("android:(?:icon|roundIcon)=\"@mipmap/([^\"]+)\"")
            .findAll(manifest)
            .map { it.groupValues[1] }
            .toSet()

        assertTrue(
            "Manifest should keep launcher icon refs in @mipmap so density bitmaps and adaptive icons both resolve correctly.",
            launcherRefs.isNotEmpty(),
        )

        launcherRefs.forEach { resourceName ->
            val adaptiveIcon = File(projectDir, "app/src/main/res/mipmap-anydpi-v26/$resourceName.xml")
            assertTrue(
                "Android 8+ launchers should resolve @mipmap/$resourceName to an adaptive icon resource. Expected: ${adaptiveIcon.relativeTo(projectDir)}",
                adaptiveIcon.isFile,
            )

            val xml = adaptiveIcon.readText()
            assertTrue("$resourceName adaptive icon XML should use an <adaptive-icon> root.", xml.contains("<adaptive-icon"))
            assertTrue("$resourceName adaptive icon XML should define a background layer.", xml.contains("<background"))
            assertTrue("$resourceName adaptive icon XML should define a foreground layer with safe-zone padding.", xml.contains("<foreground"))

            val backgroundRef = Regex("<background[^>]*android:drawable=\"([^\"]+)\"")
                .find(xml)
                ?.groupValues
                ?.get(1)
            assertTrue(
                "$resourceName adaptive icon background must resolve to transparent; launcher icons should not show the previous opaque dark tile.",
                backgroundRef != null && isTransparentDrawableReference(backgroundRef),
            )
        }
    }

    private fun readPngSize(file: File): PngSize {
        val bytes = file.readBytes()
        assertPngSignature(bytes, file)
        return PngSize(width = bytes.readBigEndianInt(16), height = bytes.readBigEndianInt(20))
    }

    private fun readPngImage(file: File): PngImage {
        assertTrue("Expected PNG file to exist: ${file.relativeTo(projectDir)}", file.isFile)
        val bytes = file.readBytes()
        assertPngSignature(bytes, file)

        var width = 0
        var height = 0
        var bitDepth = 0
        var colorType = 0
        var compressionMethod = 0
        var filterMethod = 0
        var interlaceMethod = 0
        val idatChunks = mutableListOf<ByteArray>()

        var offset = 8
        chunkLoop@ while (offset < bytes.size) {
            val chunkLength = bytes.readBigEndianInt(offset)
            val chunkType = bytes.copyOfRange(offset + 4, offset + 8).toString(Charsets.US_ASCII)
            val dataStart = offset + 8
            val dataEnd = dataStart + chunkLength
            assertTrue("PNG chunk overruns file: ${file.relativeTo(projectDir)}", dataEnd + 4 <= bytes.size)

            when (chunkType) {
                "IHDR" -> {
                    width = bytes.readBigEndianInt(dataStart)
                    height = bytes.readBigEndianInt(dataStart + 4)
                    bitDepth = bytes[dataStart + 8].toInt() and 0xFF
                    colorType = bytes[dataStart + 9].toInt() and 0xFF
                    compressionMethod = bytes[dataStart + 10].toInt() and 0xFF
                    filterMethod = bytes[dataStart + 11].toInt() and 0xFF
                    interlaceMethod = bytes[dataStart + 12].toInt() and 0xFF
                }
                "IDAT" -> idatChunks += bytes.copyOfRange(dataStart, dataEnd)
                "IEND" -> break@chunkLoop
            }

            offset = dataEnd + 4
        }

        assertTrue("PNG IHDR should define a positive width: ${file.relativeTo(projectDir)}", width > 0)
        assertTrue("PNG IHDR should define a positive height: ${file.relativeTo(projectDir)}", height > 0)
        assertTrue("Launcher icon PNGs should use 8-bit channels: ${file.relativeTo(projectDir)}", bitDepth == 8)
        assertTrue("Launcher icon PNGs should use standard zlib compression: ${file.relativeTo(projectDir)}", compressionMethod == 0)
        assertTrue("Launcher icon PNGs should use standard PNG filtering: ${file.relativeTo(projectDir)}", filterMethod == 0)
        assertTrue("Launcher icon PNGs should not be interlaced so density assets stay deterministic: ${file.relativeTo(projectDir)}", interlaceMethod == 0)

        return PngImage(
            width = width,
            height = height,
            hasAlphaChannel = colorType == PNG_COLOR_TYPE_GRAYSCALE_ALPHA || colorType == PNG_COLOR_TYPE_TRUECOLOR_ALPHA,
            alphas = decodeAlphaChannel(file, width, height, colorType, idatChunks),
        )
    }

    private fun decodeAlphaChannel(
        file: File,
        width: Int,
        height: Int,
        colorType: Int,
        idatChunks: List<ByteArray>,
    ): IntArray {
        val bytesPerPixel = when (colorType) {
            PNG_COLOR_TYPE_TRUECOLOR -> 3
            PNG_COLOR_TYPE_GRAYSCALE_ALPHA -> 2
            PNG_COLOR_TYPE_TRUECOLOR_ALPHA -> 4
            else -> error("Unsupported launcher icon PNG color type $colorType in ${file.relativeTo(projectDir)}")
        }
        val stride = width * bytesPerPixel
        val compressed = ByteArray(idatChunks.sumOf { it.size })
        var compressedOffset = 0
        idatChunks.forEach { chunk ->
            chunk.copyInto(compressed, compressedOffset)
            compressedOffset += chunk.size
        }
        val inflated = InflaterInputStream(ByteArrayInputStream(compressed)).readBytes()
        assertTrue(
            "Decoded PNG data should include one filter byte plus pixel bytes per row: ${file.relativeTo(projectDir)}",
            inflated.size >= height * (stride + 1),
        )

        val alphas = IntArray(width * height)
        val previous = ByteArray(stride)
        val current = ByteArray(stride)
        var inflatedOffset = 0

        for (y in 0 until height) {
            val filterType = inflated[inflatedOffset++].toInt() and 0xFF
            for (i in 0 until stride) {
                val raw = inflated[inflatedOffset++].toInt() and 0xFF
                val left = if (i >= bytesPerPixel) current[i - bytesPerPixel].toInt() and 0xFF else 0
                val up = previous[i].toInt() and 0xFF
                val upLeft = if (i >= bytesPerPixel) previous[i - bytesPerPixel].toInt() and 0xFF else 0
                val decoded = when (filterType) {
                    0 -> raw
                    1 -> raw + left
                    2 -> raw + up
                    3 -> raw + ((left + up) / 2)
                    4 -> raw + paethPredictor(left, up, upLeft)
                    else -> error("Unsupported PNG filter type $filterType in ${file.relativeTo(projectDir)}")
                }
                current[i] = (decoded and 0xFF).toByte()
            }

            for (x in 0 until width) {
                alphas[y * width + x] = when (colorType) {
                    PNG_COLOR_TYPE_TRUECOLOR_ALPHA -> current[x * bytesPerPixel + 3].toInt() and 0xFF
                    PNG_COLOR_TYPE_GRAYSCALE_ALPHA -> current[x * bytesPerPixel + 1].toInt() and 0xFF
                    PNG_COLOR_TYPE_TRUECOLOR -> 255
                    else -> error("Unsupported launcher icon PNG color type $colorType in ${file.relativeTo(projectDir)}")
                }
            }
            current.copyInto(previous)
        }

        return alphas
    }

    private fun paethPredictor(left: Int, up: Int, upLeft: Int): Int {
        val estimate = left + up - upLeft
        val distanceLeft = kotlin.math.abs(estimate - left)
        val distanceUp = kotlin.math.abs(estimate - up)
        val distanceUpLeft = kotlin.math.abs(estimate - upLeft)
        return when {
            distanceLeft <= distanceUp && distanceLeft <= distanceUpLeft -> left
            distanceUp <= distanceUpLeft -> up
            else -> upLeft
        }
    }

    private fun assertVisibleArtworkWidth(
        file: File,
        minWidthRatio: Double,
        maxWidthRatio: Double,
        reason: String,
    ) {
        val image = readPngImage(file)
        val bounds = requireNotNull(image.nonTransparentBounds()) {
            "${file.relativeTo(projectDir)} should contain visible cassette artwork."
        }
        val widthRatio = bounds.width.toDouble() / image.width.toDouble()
        val heightRatio = bounds.height.toDouble() / image.height.toDouble()
        val relativePath = file.relativeTo(projectDir)

        assertTrue(
            "$relativePath visible artwork width ratio should be >= $minWidthRatio (${"%.3f".format(widthRatio)} actual). $reason",
            widthRatio >= minWidthRatio,
        )
        assertTrue(
            "$relativePath visible artwork width ratio should be <= $maxWidthRatio (${"%.3f".format(widthRatio)} actual) to prove the cassette is not clipped to a full-canvas opaque square. $reason",
            widthRatio <= maxWidthRatio,
        )
        assertTrue(
            "$relativePath visible artwork height should retain meaningful cassette detail (${"%.3f".format(heightRatio)} actual).",
            heightRatio >= 0.40,
        )
        assertTrue(
            "$relativePath visible artwork height should leave transparent edge padding (${"%.3f".format(heightRatio)} actual).",
            heightRatio <= 0.95,
        )
    }

    private fun isTransparentDrawableReference(drawableRef: String): Boolean {
        if (isTransparentColorValue(drawableRef)) return true

        if (drawableRef.startsWith("@color/")) {
            val colorName = drawableRef.substringAfter("@color/")
            return resolveColorValue(colorName)?.let(::isTransparentColorValue) == true
        }

        if (drawableRef.startsWith("@drawable/")) {
            val drawableName = drawableRef.substringAfter("@drawable/")
            val drawable = File(projectDir, "app/src/main/res/drawable/$drawableName.xml")
            return drawable.isFile && drawable.readText().lineSequence().any { line ->
                line.contains("@android:color/transparent") || Regex("#00[0-9A-Fa-f]{6}").containsMatchIn(line)
            }
        }

        return false
    }

    private fun resolveColorValue(colorName: String): String? {
        val colorsXml = File(projectDir, "app/src/main/res/values/colors.xml")
        if (!colorsXml.isFile) return null
        return Regex("""<color\s+name="${Regex.escape(colorName)}"\s*>([^<]+)</color>""")
            .find(colorsXml.readText())
            ?.groupValues
            ?.get(1)
            ?.trim()
    }

    private fun isTransparentColorValue(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed == "@android:color/transparent" ||
            Regex("#[0-9A-Fa-f]{8}").matches(trimmed) && trimmed.substring(1, 3) == "00" ||
            Regex("#[0-9A-Fa-f]{4}").matches(trimmed) && trimmed[1] == '0'
    }

    private fun assertPngSignature(bytes: ByteArray, file: File) {
        assertTrue("PNG file is too short: ${file.relativeTo(projectDir)}", bytes.size >= 24)
        assertArrayEquals(
            "Launcher icon must have a PNG signature: ${file.relativeTo(projectDir)}",
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            bytes.copyOfRange(0, 8),
        )
    }

    private fun ByteArray.readBigEndianInt(offset: Int): Int =
        ((this[offset].toInt() and 0xFF) shl 24) or
            ((this[offset + 1].toInt() and 0xFF) shl 16) or
            ((this[offset + 2].toInt() and 0xFF) shl 8) or
            (this[offset + 3].toInt() and 0xFF)

    private data class PngSize(val width: Int, val height: Int)

    private data class PixelBounds(val width: Int, val height: Int)

    private data class PngImage(
        val width: Int,
        val height: Int,
        val hasAlphaChannel: Boolean,
        val alphas: IntArray,
    ) {
        fun alphaAt(x: Int, y: Int): Int = alphas[y * width + x]

        fun cornerAlphas(): List<Int> = listOf(
            alphaAt(0, 0),
            alphaAt(width - 1, 0),
            alphaAt(0, height - 1),
            alphaAt(width - 1, height - 1),
        )

        fun nonTransparentBounds(): PixelBounds? {
            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1

            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (alphaAt(x, y) > 0) {
                        minX = minOf(minX, x)
                        minY = minOf(minY, y)
                        maxX = maxOf(maxX, x)
                        maxY = maxOf(maxY, y)
                    }
                }
            }

            if (maxX < minX || maxY < minY) return null
            return PixelBounds(width = maxX - minX + 1, height = maxY - minY + 1)
        }
    }

    private fun findProjectDir(start: File): File {
        var current: File? = start
        while (current != null) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile
        }
        error("Unable to locate android-mixtape project root from ${start.absolutePath}")
    }

    private companion object {
        const val PNG_COLOR_TYPE_TRUECOLOR = 2
        const val PNG_COLOR_TYPE_GRAYSCALE_ALPHA = 4
        const val PNG_COLOR_TYPE_TRUECOLOR_ALPHA = 6
    }
}
