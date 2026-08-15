package com.example.androidmixtape.compat

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Api19VariantContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val moduleDir = File(projectDir, "app")
    private val buildScript = File(moduleDir, "build.gradle.kts").readText()

    @Test
    fun gradleDeclaresModernAndLegacyDebugVariants() {
        assertTrue(
            "Define product flavors so modernDebug can keep the current API 24+ Compose app while legacyDebug can install on API 19 devices.",
            buildScript.contains("productFlavors"),
        )
        assertTrue("Add a modern flavor for the existing Nokia G60 / Android 14 path.", buildScript.contains("create(\"modern\")"))
        assertTrue("Add a legacy flavor for Samsung SM-G360G / Android 4.4.4.", buildScript.contains("create(\"legacy\")"))
        assertTrue("legacyDebug must declare minSdk = 19.", Regex("create\\(\\\"legacy\\\"\\).*?minSdk\\s*=\\s*19", RegexOption.DOT_MATCHES_ALL).containsMatchIn(buildScript))
        assertTrue("modernDebug should preserve the current minSdk = 24 behavior.", Regex("create\\(\\\"modern\\\"\\).*?minSdk\\s*=\\s*24", RegexOption.DOT_MATCHES_ALL).containsMatchIn(buildScript))
    }

    @Test
    fun api21PlusUiAndPlaybackDependenciesAreModernOnly() {
        assertFalse(
            "Shared implementation dependencies must not include Compose because Compose cannot run on Android 4.4/API 19.",
            buildScript.contains("implementation(platform(\"androidx.compose") || buildScript.contains("implementation(\"androidx.compose"),
        )
        assertFalse(
            "Shared implementation dependencies must not include activity-compose; keep it in modernImplementation.",
            buildScript.contains("implementation(\"androidx.activity:activity-compose"),
        )
        assertFalse(
            "Shared implementation dependencies must not include lifecycle compose artifacts; keep them in modernImplementation.",
            buildScript.contains("implementation(\"androidx.lifecycle:lifecycle-runtime-compose") ||
                buildScript.contains("implementation(\"androidx.lifecycle:lifecycle-viewmodel-compose"),
        )
        assertFalse(
            "Shared implementation dependencies must not include Media3/ExoPlayer; legacy should use an API-19-safe playback implementation.",
            buildScript.contains("implementation(\"androidx.media3:"),
        )

        assertTrue("Compose BOM should move to modernImplementation.", buildScript.contains("modernImplementation(platform(\"androidx.compose"))
        assertTrue("Media3 ExoPlayer should move to modernImplementation.", buildScript.contains("modernImplementation(\"androidx.media3:media3-exoplayer"))
    }

    @Test
    fun sourceSetsSeparateModernComposeFromLegacyViews() {
        assertFileExists("app/src/modern/java/com/example/androidmixtape/MainActivity.kt", "Move the existing Compose MainActivity to the modern source set.")
        assertFileExists("app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt", "Keep Media3 playback in the modern source set.")
        assertFileExists("app/src/legacy/java/com/example/androidmixtape/MainActivity.kt", "Add a legacy plain-Views MainActivity for API 19.")
        assertFileExists("app/src/legacy/java/com/example/androidmixtape/playback/PlatformMediaPlayerEngine.kt", "Add a legacy platform MediaPlayer engine for API 19 content:// playback.")
    }

    private fun assertFileExists(relativePath: String, message: String) {
        assertTrue("$message Expected file: $relativePath", File(projectDir, relativePath).isFile)
    }

    private fun findProjectDir(start: File): File {
        var current: File? = start
        while (current != null) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile
        }
        error("Unable to locate android-mixtape project root from ${start.absolutePath}")
    }
}
