package com.example.androidmixtape.auto

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoSupportContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val moduleDir = File(projectDir, "app")
    private val buildScript = File(moduleDir, "build.gradle.kts").readText()

    @Test
    fun modernAppDeclaresCarsAppLibraryAndMediaSession() {
        assertTrue(
            "The custom car UI must use the Android for Cars App Library in the modern flavor.",
            buildScript.contains("modernImplementation(\"androidx.car.app:app"),
        )

        assertTrue(
            "Android Auto/AAOS media plumbing still requires a Media3 session/library service dependency in the modern flavor.",
            buildScript.contains("modernImplementation(\"androidx.media3:media3-session"),
        )
    }

    @Test
    fun modernFlavorAdvertisesTemplatedMediaCarApp() {
        val automotiveDescription = File(moduleDir, "src/modern/res/xml/automotive_app_desc.xml")
        assertTrue(
            "Keep app/src/modern/res/xml/automotive_app_desc.xml so Android Auto recognizes Mixtape as a media app.",
            automotiveDescription.isFile,
        )
        val automotiveDescriptionText = automotiveDescription.takeIf { it.isFile }?.readText().orEmpty()
        assertTrue(
            "automotive_app_desc.xml must declare an automotiveApp root.",
            automotiveDescriptionText.contains("<automotiveApp"),
        )
        assertTrue(
            "automotive_app_desc.xml must keep <uses name=\"media\"/> for media discovery and playback plumbing.",
            Regex("<uses\\s+name=\\\"media\\\"\\s*/?>").containsMatchIn(automotiveDescriptionText),
        )
        assertTrue(
            "automotive_app_desc.xml must also include <uses name=\"template\"/> so the custom Mixtape car UI is discovered instead of only the stock media browser.",
            Regex("<uses\\s+name=\\\"template\\\"\\s*/?>").containsMatchIn(automotiveDescriptionText),
        )

        val manifest = modernManifestText()
        assertTrue(
            "Modern manifest overlay must reference com.google.android.gms.car.application metadata.",
            manifest.contains("com.google.android.gms.car.application"),
        )
        assertTrue(
            "Modern manifest overlay must point Android Auto metadata at @xml/automotive_app_desc.",
            manifest.contains("@xml/automotive_app_desc"),
        )
        assertTrue(
            "Templated media apps must request androidx.car.app.MEDIA_TEMPLATES permission.",
            manifest.contains("androidx.car.app.MEDIA_TEMPLATES"),
        )
        assertTrue(
            "Templated media apps must declare androidx.car.app.minCarApiLevel metadata with value 8 or higher.",
            Regex("android:name=\\\"androidx\\.car\\.app\\.minCarApiLevel\\\"[\\s\\S]*android:value=\\\"([8-9]|[1-9][0-9]+)\\\"").containsMatchIn(manifest),
        )
        assertTrue(
            "Templated media apps must provide a tintable attribution icon.",
            manifest.contains("androidx.car.app.TintableAttributionIcon") &&
                manifest.contains("@drawable/ic_mixtape_attribution") &&
                File(moduleDir, "src/modern/res/drawable/ic_mixtape_attribution.xml").isFile,
        )
        assertTrue(
            "Declare a CarAppService entry point for the custom Mixtape car UI.",
            manifest.contains("MixtapeCarAppService"),
        )
        assertTrue(
            "MixtapeCarAppService must handle androidx.car.app.CarAppService.",
            manifest.contains("androidx.car.app.CarAppService"),
        )
        assertTrue(
            "MixtapeCarAppService must use androidx.car.app.category.MEDIA so the car host treats it as a media template app.",
            manifest.contains("androidx.car.app.category.MEDIA"),
        )
    }

    @Test
    fun modernFlavorKeepsMediaLibraryServiceOnlyAsPlaybackPlumbing() {
        val serviceSourceFile = File(moduleDir, "src/modern/java/com/example/androidmixtape/playback/MixtapeMediaLibraryService.kt")
        assertTrue(
            "Keep a modern-only MixtapeMediaLibraryService for MediaSession, voice/search, and media-browser compatibility plumbing.",
            serviceSourceFile.isFile,
        )
        val serviceSource = serviceSourceFile.takeIf { it.isFile }?.readText().orEmpty()
        assertTrue("Service should extend MediaLibraryService.", serviceSource.contains("MediaLibraryService"))
        assertTrue("Service should own/create a MediaLibrarySession.", serviceSource.contains("MediaLibrarySession"))
        assertTrue("Service should use the existing modern Media3 ExoPlayer path.", serviceSource.contains("ExoPlayer"))
        assertTrue("Service should load tracks from MediaStoreAudioRepository rather than UI state.", serviceSource.contains("MediaStoreAudioRepository"))

        val manifest = modernManifestText()
        assertTrue(
            "Modern manifest must request FOREGROUND_SERVICE for a background media playback service.",
            manifest.contains("android.permission.FOREGROUND_SERVICE"),
        )
        assertTrue(
            "Modern manifest must request FOREGROUND_SERVICE_MEDIA_PLAYBACK for target SDK 35 media services.",
            manifest.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"),
        )
        assertTrue(
            "Declare MixtapeMediaLibraryService in the modern manifest overlay.",
            manifest.contains("MixtapeMediaLibraryService"),
        )
        assertTrue("Android Auto/media browser discovery requires the service to be exported.", manifest.contains("android:exported=\"true\""))
        assertTrue("The service must declare foregroundServiceType=\"mediaPlayback\".", manifest.contains("android:foregroundServiceType=\"mediaPlayback\""))
        assertTrue(
            "Include the Media3 MediaLibraryService intent action for Media3 clients.",
            manifest.contains("androidx.media3.session.MediaLibraryService"),
        )
        assertTrue(
            "Include android.media.browse.MediaBrowserService action for Android Auto/media browser compatibility.",
            manifest.contains("android.media.browse.MediaBrowserService"),
        )
        assertFalse(
            "The media-browser compatibility tree must not leave the old root -> All tracks browse tree as Mixtape's primary car experience.",
            serviceSource.contains("ALL_TRACKS") || serviceSource.contains("All tracks"),
        )
    }

    @Test
    fun customCarUiSourceDefinesMixtapeListSideAndPlaybackFlow() {
        val source = allModernKotlinSourceText()
        assertTrue(
            "Add MixtapeCarAppService under modern source as the custom car UI entry point.",
            source.contains("class MixtapeCarAppService") && source.contains("CarAppService"),
        )
        assertTrue(
            "Add a Session whose first screen is MixtapeListScreen, making the first car view the mixtape list.",
            source.contains("class MixtapeCarSession") && source.contains("onCreateScreen") && source.contains("MixtapeListScreen"),
        )
        assertTrue(
            "The car browsing flow must include a MixtapeListScreen and MixtapeSideScreen.",
            source.contains("class MixtapeListScreen") && source.contains("class MixtapeSideScreen"),
        )
        assertTrue(
            "The custom car UI should use supported Cars App Library browsing templates such as SectionedItemTemplate/ListTemplate plus rows for mixtapes/tracks.",
            listOf("SectionedItemTemplate", "ListTemplate", "RowSection", "GridSection").any(source::contains) && source.contains("Row"),
        )
        assertTrue(
            "Playback controls must be a car-template media playback path, not only a MediaLibraryService browse result.",
            source.contains("MediaPlaybackTemplate") && source.contains("MediaPlaybackManager"),
        )
        assertTrue(
            "Browsing screens should provide a path to playback controls, for example Action.MEDIA_PLAYBACK or SHOW_MEDIA_PLAYBACK handling.",
            source.contains("Action.MEDIA_PLAYBACK") || source.contains("androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK"),
        )
    }

    @Test
    fun initialCarScreenUsesBrandedCassetteBriefcaseArtwork() {
        val source = allModernKotlinSourceText()
        assertTrue(
            "The initial car screen should use a GridSection so mixtapes read as a visual cassette collection rather than generic text rows.",
            source.contains("GridSection.Builder()") &&
                source.contains("GridSection.ITEM_SIZE_LARGE") &&
                source.contains("GridItem.Builder()"),
        )
        assertTrue(
            "Every car mixtape grid item should include generated cassette-spine artwork.",
            source.contains("MixtapeCarArtworkRenderer.icon") &&
                source.contains("GridItem.IMAGE_TYPE_LARGE") &&
                source.contains("SharedPreferencesMixtapeVisualPropertiesStore") &&
                source.contains("visualPropertiesStore.propertiesFor(stableKey)"),
        )
        assertTrue(
            "Keep the branded Mixtape briefcase title while the phone header stays concise.",
            source.contains("\"${'$'}{groups.size} mix tapes\"") &&
                source.contains("mix tapes filed spine-out in a cassette briefcase") &&
                source.contains("Mixtape briefcase"),
        )
    }

    @Test
    fun docsAndTestsDoNotPresentAllTracksAsThePrimaryAndroidAutoExperience() {
        val userFacingDocs = File(projectDir, "README.md").readText() + "\n" + File(projectDir, "TEST_PLAN.md").readText()
        assertFalse(
            "README/TEST_PLAN must be updated away from the card-2375 stock 'All tracks' Android Auto browse-root description.",
            userFacingDocs.contains("All tracks"),
        )

        val thisContract = File(moduleDir, "src/test/java/com/example/androidmixtape/auto/AndroidAutoSupportContractTest.kt").canonicalFile
        val testText = File(moduleDir, "src/test").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.canonicalFile != thisContract }
            .joinToString("\n") { it.readText() }
        assertFalse(
            "Tests must no longer define success as the old root -> All tracks media-browser tree.",
            testText.contains("root -> All tracks") || testText.contains("Root/All tracks"),
        )
    }

    private fun modernManifestText(): String {
        val manifest = File(moduleDir, "src/modern/AndroidManifest.xml")
        assertTrue(
            "Add app/src/modern/AndroidManifest.xml for Android Auto metadata and service declarations.",
            manifest.isFile,
        )
        return manifest.takeIf { it.isFile }?.readText().orEmpty()
    }

    private fun allModernKotlinSourceText(): String = File(moduleDir, "src/modern/java")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .joinToString("\n") { it.readText() }

    private fun findProjectDir(start: File): File {
        var current: File? = start
        while (current != null) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile
        }
        error("Unable to locate android-mixtape project root from ${start.absolutePath}")
    }
}
