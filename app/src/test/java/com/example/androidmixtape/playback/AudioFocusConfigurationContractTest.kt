package com.example.androidmixtape.playback

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusConfigurationContractTest {
    @Test fun android15BackgroundPlayPromotesBeforeExoPlayerRequestsFocus() {
        val path = "src/modern/java/com/example/androidmixtape/playback/MixtapeMediaLibraryService.kt"
        val source = listOf(File("app/$path"), File(path)).first { it.exists() }.readText()
        assertTrue(source.contains("object : ForwardingPlayer(player)"))
        assertTrue(source.contains("if (prepareForegroundForAudioFocus()) super.play()"))
        assertTrue(source.contains("if (!playWhenReady || prepareForegroundForAudioFocus()) super.setPlayWhenReady(playWhenReady)"))
        assertTrue(source.contains("if (Build.VERSION.SDK_INT < 35) return true"))
        assertTrue(source.contains("onUpdateNotification(session, /* startInForegroundRequired = */ true)"))
    }

    @Test fun sharedServiceAndStandalonePlayerRequestMusicFocus() {
        listOf("MixtapeMediaLibraryService.kt", "ExoPlayerEngine.kt").forEach { name ->
            val path = "src/modern/java/com/example/androidmixtape/playback/$name"
            val source = listOf(File("app/$path"), File(path)).first { it.exists() }.readText()
            assertTrue("$name must use media usage", source.contains(".setUsage(C.USAGE_MEDIA)"))
            assertTrue("$name must identify music content", source.contains(".setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)"))
            assertTrue("$name must let ExoPlayer manage audio focus, not leave it disabled by default",
                Regex("setAudioAttributes\\([\\s\\S]*?/\\* handleAudioFocus = \\*/ true").containsMatchIn(source))
        }
    }
}
