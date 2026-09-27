package com.example.androidmixtape.auto

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCustomCarUiFlowContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val moduleDir = File(projectDir, "app")
    private val modernSource = File(moduleDir, "src/modern/java")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .joinToString("\n") { it.readText() }

    @Test
    fun sessionLaunchesDirectlyToMixtapeList() {
        assertTrue(
            "MixtapeCarSession.onCreateScreen must return MixtapeListScreen so the first car view is the mixtape list.",
            Regex("class\\s+MixtapeCarSession[\\s\\S]*onCreateScreen[\\s\\S]*MixtapeListScreen").containsMatchIn(modernSource),
        )
    }

    @Test
    fun mixtapeRowsNavigateToSideOfTapeView() {
        assertTrue(
            "MixtapeListScreen should render the car catalog's mixtapes as rows/list items.",
            Regex("class\\s+MixtapeListScreen[\\s\\S]*(CarMixtape|mixtapes)[\\s\\S]*(Row|Item)").containsMatchIn(modernSource),
        )
        assertTrue(
            "Selecting a mixtape row must push/open MixtapeSideScreen for that mixtape.",
            Regex("MixtapeListScreen[\\s\\S]*(screenManager|ScreenManager|push|replace)[\\s\\S]*MixtapeSideScreen").containsMatchIn(modernSource),
        )
    }

    @Test
    fun sideViewShowsTracksAndStartsSharedSessionPlayback() {
        assertTrue(
            "MixtapeSideScreen should render the selected side-of-tape track list from the chosen CarMixtape.",
            Regex("class\\s+MixtapeSideScreen[\\s\\S]*(tracks|Track)[\\s\\S]*(Row|Item)").containsMatchIn(modernSource),
        )
        assertTrue(
            "Side/track playback actions must target the shared MixtapeMediaLibraryService session rather than a separate car-only player.",
            modernSource.contains("MixtapeMediaLibraryService") &&
                (modernSource.contains("MediaController") || modernSource.contains("SessionToken")) &&
                (modernSource.contains("setMediaItems") || modernSource.contains("playMixtape") || modernSource.contains("playSide")),
        )
        assertTrue(
            "Selecting track N in a side should preserve the selected start index when loading the shared session queue.",
            modernSource.contains("startIndex") || modernSource.contains("selectedTrackIndex") || modernSource.contains("trackIndex"),
        )
    }

    @Test
    fun playbackScreenUsesMediaPlaybackTemplateWithRegisteredSessionToken() {
        assertTrue(
            "Custom car playback must be exposed through MediaPlaybackTemplate.",
            modernSource.contains("MediaPlaybackTemplate"),
        )
        assertTrue(
            "The Cars App Library MediaPlaybackManager must be given the shared MediaSession token so car controls stay coherent with phone playback.",
            modernSource.contains("MediaPlaybackManager") && modernSource.contains("registerMediaPlaybackToken"),
        )
        assertTrue(
            "The implementation should handle the car host's SHOW_MEDIA_PLAYBACK action or expose Action.MEDIA_PLAYBACK from browsing screens.",
            modernSource.contains("androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK") || modernSource.contains("Action.MEDIA_PLAYBACK"),
        )
    }

    @Test
    fun carCatalogReusesPhoneMixtapeGroupingWithStableMediaIds() {
        assertTrue(
            "Add a shared car catalog model such as CarMixtape so car UI can load mixtapes without constructing Compose UI state.",
            modernSource.contains("CarMixtape") && modernSource.contains("CarMixtapeCatalog"),
        )
        assertTrue(
            "The car catalog must resolve the phone's saved membership so removals cannot regroup or rename tapes in the car.",
            modernSource.contains("resolveMixtapeGroups") && modernSource.contains("SharedPreferencesMixtapeMembershipStore"),
        )
        assertTrue(
            "Car catalog media IDs should be stable and distinguish mixtapes from tracks.",
            modernSource.contains("mixtape:") && modernSource.contains("track:"),
        )
        assertTrue(
            "Permission-denied or unavailable MediaStore access should produce a safe empty/permission-needed car catalog state.",
            modernSource.contains("SecurityException") &&
                (modernSource.contains("Permission") || modernSource.contains("permission")) &&
                (modernSource.contains("emptyList()") || modernSource.contains("needsPermission")),
        )
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
