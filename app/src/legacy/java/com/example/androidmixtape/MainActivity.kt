package com.example.androidmixtape

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.example.androidmixtape.data.MediaStoreAudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.permissions.AudioPermissionPolicy
import com.example.androidmixtape.playback.MixtapeController
import com.example.androidmixtape.playback.PlatformMediaPlayerEngine
import kotlinx.coroutines.runBlocking

class MainActivity : Activity() {
    private data class RetainedPlaybackState(
        val controller: MixtapeController,
        val tracks: List<Track>,
    )

    private val permissionRequestCode = 19
    private lateinit var statusText: TextView
    private lateinit var trackList: ListView
    private lateinit var playPauseButton: Button
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var controller: MixtapeController
    private lateinit var repository: MediaStoreAudioRepository
    private var tracks: List<Track> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val retainedState = lastNonConfigurationInstance as? RetainedPlaybackState
        repository = MediaStoreAudioRepository(applicationContext)
        controller = retainedState?.controller ?: MixtapeController(PlatformMediaPlayerEngine(applicationContext))
        tracks = retainedState?.tracks ?: emptyList()
        buildUi()
        controller.setOnPlaybackStateChanged {
            runOnUiThread { updatePlaybackStatus() }
        }
        if (hasAudioAccess()) {
            if (retainedState == null) refreshLibrary() else restoreRetainedLibrary()
        } else {
            showPermissionRequired()
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        statusText = TextView(this).apply {
            text = "Scanning device audio…"
            textSize = 18f
        }
        root.addView(statusText)

        val topButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        topButtons.addView(Button(this).apply {
            text = "Refresh"
            setOnClickListener { if (hasAudioAccess()) refreshLibrary() else requestAudioAccess() }
        })
        topButtons.addView(Button(this).apply {
            text = "Stop"
            setOnClickListener {
                controller.stop()
                updatePlaybackStatus()
            }
        })
        root.addView(topButtons)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        trackList = ListView(this).apply {
            adapter = this@MainActivity.adapter
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ -> playTrack(position) }
        }
        root.addView(trackList, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val transport = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        previousButton = Button(this).apply {
            text = "Previous"
            setOnClickListener {
                controller.previous()
                updatePlaybackStatus()
            }
        }
        playPauseButton = Button(this).apply {
            text = "Play/Pause"
            setOnClickListener {
                controller.togglePlayPause()
                updatePlaybackStatus()
            }
        }
        nextButton = Button(this).apply {
            text = "Next"
            setOnClickListener {
                controller.next()
                updatePlaybackStatus()
            }
        }
        transport.addView(previousButton)
        transport.addView(playPauseButton)
        transport.addView(nextButton)
        root.addView(transport)

        setContentView(root)
    }

    private fun hasAudioAccess(): Boolean {
        val runtimePermission = AudioPermissionPolicy.requiredRuntimePermission(Build.VERSION.SDK_INT) ?: return true
        return if (Build.VERSION.SDK_INT >= 23) {
            checkSelfPermission(runtimePermission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showPermissionRequired() {
        statusText.text = "Mixtape needs storage permission to find songs on this device. Tap Refresh to allow access."
    }

    private fun requestAudioAccess() {
        val runtimePermission = AudioPermissionPolicy.requiredRuntimePermission(Build.VERSION.SDK_INT)
        if (runtimePermission == null) {
            refreshLibrary()
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(arrayOf(runtimePermission), permissionRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            refreshLibrary()
        } else {
            showPermissionRequired()
        }
    }

    private fun restoreRetainedLibrary() {
        adapter.clear()
        adapter.addAll(tracks.map { "${it.title} — ${it.displaySubtitle}" })
        adapter.notifyDataSetChanged()
        updatePlaybackStatus()
    }

    private fun refreshLibrary() {
        statusText.text = "Scanning device audio…"
        Thread {
            val result = runCatching { runBlocking { repository.loadTracks() } }
            runOnUiThread {
                result.onSuccess { loadedTracks ->
                    tracks = loadedTracks
                    controller.load(tracks)
                    adapter.clear()
                    adapter.addAll(tracks.map { "${it.title} — ${it.displaySubtitle}" })
                    adapter.notifyDataSetChanged()
                    statusText.text = if (tracks.isEmpty()) {
                        "No audio files found. Add MP3, M4A, OGG, or WAV files to Music and refresh."
                    } else {
                        "${tracks.size} local tracks ready"
                    }
                    updateTransportEnabledState()
                }.onFailure { error ->
                    statusText.text = "Audio scan failed: ${error.message ?: "unknown error"}"
                }
            }
        }.start()
    }

    private fun playTrack(position: Int) {
        if (position !in tracks.indices) return
        runCatching {
            controller.load(tracks)
            controller.select(position)
        }.onSuccess {
            updatePlaybackStatus()
        }.onFailure { error ->
            statusText.text = "Playback failed: ${error.message ?: "unknown error"}"
        }
    }

    private fun updatePlaybackStatus() {
        val current = controller.state.currentTrack
        statusText.text = when {
            current == null && tracks.isEmpty() -> "No audio files found."
            current == null -> "${tracks.size} local tracks ready"
            controller.state.isPlaying -> "Playing ${current.title}"
            else -> "Paused ${current.title}"
        }
        updateTransportEnabledState()
    }

    private fun updateTransportEnabledState() {
        val hasTrack = controller.state.currentTrack != null
        playPauseButton.isEnabled = hasTrack
        previousButton.isEnabled = controller.state.canGoPrevious
        nextButton.isEnabled = controller.state.canGoNext
    }

    override fun onRetainNonConfigurationInstance(): Any = RetainedPlaybackState(controller, tracks)

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            controller.release()
        }
        super.onDestroy()
    }
}
