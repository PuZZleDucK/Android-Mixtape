package com.example.androidmixtape.playback

import android.content.Context
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.MediaStoreAudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.diagnostics.AndroidAutoDiagnostics
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MixtapeMediaLibraryService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        AndroidAutoDiagnostics.log(this, "media_library_service_onCreate")
        AndroidAutoDiagnostics.logPhoneEnvironment(this, "media_library_service_create")
        val meteringAudioProcessor = MeteringAudioProcessor()
        val player = ExoPlayer.Builder(
            this,
            MeteringRenderersFactory(this, meteringAudioProcessor),
        ).build().apply {
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val mediaTree = AndroidAutoMediaTree(MediaStoreAudioRepository(applicationContext))
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            MixtapeLibraryCallback(applicationContext, mediaTree),
        ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        AndroidAutoDiagnostics.log(this, "media_library_onGetSession ${AndroidAutoDiagnostics.controller(controllerInfo)}")
        return mediaLibrarySession
    }

    override fun onDestroy() {
        AndroidAutoDiagnostics.log(this, "media_library_service_onDestroy")
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        AudioLevelMonitor.reset()
        super.onDestroy()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private class MixtapeLibraryCallback(
        private val context: Context,
        private val mediaTree: AndroidAutoMediaTree,
    ) : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            AndroidAutoDiagnostics.log(context, "media_library_onConnect ${AndroidAutoDiagnostics.controller(controller)}")
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(GET_SESSION_COMPAT_TOKEN_COMMAND)
                .build()
            return MediaSession.ConnectionResult.accept(commands, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onCustomCommand action=${customCommand.customAction} ${AndroidAutoDiagnostics.controller(controller)}",
            )
            if (customCommand.customAction != ACTION_GET_SESSION_COMPAT_TOKEN) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            val extras = Bundle().apply {
                putParcelable(EXTRA_SESSION_COMPAT_TOKEN, session.sessionCompatToken)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onGetLibraryRoot ${AndroidAutoDiagnostics.controller(browser)} params=$params",
            )
            return Futures.immediateFuture(LibraryResult.ofItem(mediaTree.root(), params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = mediaTree.children(parentId, page, pageSize)
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onGetChildren parentId=$parentId page=$page pageSize=$pageSize returned=${children.size} " +
                    AndroidAutoDiagnostics.controller(browser),
            )
            return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
        }

        @UnstableApi
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onGetItem mediaId=$mediaId ${AndroidAutoDiagnostics.controller(browser)}",
            )
            return Futures.immediateFuture(
                mediaTree.item(mediaId)?.let { LibraryResult.ofItem(it, null) }
                    ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onAddMediaItems requested=${mediaItems.size} ids=${mediaItems.diagnosticIds()} " +
                    AndroidAutoDiagnostics.controller(controller),
            )
            return Futures.immediateFuture(mediaTree.resolvePlayableMediaItems(mediaItems))
        }

        @UnstableApi
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onSetMediaItems requested=${mediaItems.size} ids=${mediaItems.diagnosticIds()} " +
                    "startIndex=$startIndex startPositionMs=$startPositionMs ${AndroidAutoDiagnostics.controller(controller)}",
            )
            val resolvedQueue = mediaTree.resolvePlayableQueue(mediaItems)
            val requestedId = mediaItems.getOrNull(startIndex.coerceAtLeast(0))?.mediaId
            val resolvedStartIndex = if (resolvedQueue.isEmpty()) {
                C.INDEX_UNSET
            } else {
                resolvedQueue.indexOfFirst { it.mediaId == requestedId }
                    .takeIf { it >= 0 }
                    ?: startIndex.coerceIn(0, resolvedQueue.lastIndex)
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolvedQueue, resolvedStartIndex, startPositionMs),
            )
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onSearch queryLength=${query.length} ${AndroidAutoDiagnostics.controller(browser)}",
            )
            session.notifySearchResultChanged(browser, query, mediaTree.searchTrackIds(query).size, params)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val results = mediaTree.paginate(mediaTree.search(query), page, pageSize)
            AndroidAutoDiagnostics.log(
                context,
                "media_library_onGetSearchResult queryLength=${query.length} page=$page pageSize=$pageSize returned=${results.size} " +
                    AndroidAutoDiagnostics.controller(browser),
            )
            return Futures.immediateFuture(LibraryResult.ofItemList(results, params))
        }
    }

    companion object {
        private fun List<MediaItem>.diagnosticIds(): String {
            val shown = take(20).map { it.mediaId }
            return if (size > shown.size) "$shown (+${size - shown.size} more)" else shown.toString()
        }

        const val ACTION_GET_SESSION_COMPAT_TOKEN = "com.example.androidmixtape.GET_SESSION_COMPAT_TOKEN"
        const val EXTRA_SESSION_COMPAT_TOKEN = "com.example.androidmixtape.EXTRA_SESSION_COMPAT_TOKEN"
        val GET_SESSION_COMPAT_TOKEN_COMMAND = SessionCommand(ACTION_GET_SESSION_COMPAT_TOKEN, Bundle.EMPTY)
    }
}

class AndroidAutoMediaTree(
    private val repository: AudioRepository,
) {
    fun root(): MediaItem = browsableItem(ROOT_ID, "Mixtape compatibility library")

    fun children(parentId: String, page: Int, pageSize: Int): List<MediaItem> = when (parentId) {
        ROOT_ID -> paginate(loadPlayableTracks(), page, pageSize)
        else -> emptyList()
    }

    fun item(mediaId: String): MediaItem? = when (mediaId) {
        ROOT_ID -> root()
        else -> loadPlayableTracks().firstOrNull { it.mediaId == mediaId }
    }

    fun search(query: String): List<MediaItem> = searchTracks(query).map(::trackItem)

    fun searchTrackIds(query: String): List<String> = searchTracks(query).map { it.mediaId }

    fun mediaIdFor(track: Track): String = track.mediaId

    private fun searchTracks(query: String): List<Track> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        return loadTracksSafely()
            .filter { track ->
                listOf(track.title, track.artist, track.album.orEmpty(), track.displayName.orEmpty())
                    .any { candidate -> candidate.contains(normalizedQuery, ignoreCase = true) }
            }
    }

    fun resolvePlayableMediaItems(mediaItems: List<MediaItem>): List<MediaItem> = mediaItems.mapNotNull { requestedItem ->
        if (requestedItem.localConfiguration?.uri != null) {
            requestedItem
        } else {
            item(requestedItem.mediaId)?.takeIf { it.mediaMetadata.isPlayable == true }
        }
    }

    fun resolvePlayableQueue(mediaItems: List<MediaItem>): List<MediaItem> {
        if (mediaItems.isEmpty()) return emptyList()
        val firstRequestedId = mediaItems.first().mediaId
        if (mediaItems.size == 1 && (firstRequestedId == ROOT_ID || firstRequestedId.startsWith(TRACK_PREFIX))) {
            return loadPlayableTracks()
        }
        return resolvePlayableMediaItems(mediaItems)
    }

    fun paginate(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (page < 0 || pageSize == 0) return items
        if (pageSize < 0) return emptyList()
        val fromIndex = page * pageSize
        if (fromIndex >= items.size) return emptyList()
        return items.subList(fromIndex, minOf(fromIndex + pageSize, items.size))
    }

    private fun loadPlayableTracks(): List<MediaItem> = loadTracksSafely().map(::trackItem)

    private fun loadTracksSafely(): List<Track> = try {
        runBlocking(Dispatchers.IO) {
            repository.loadTracks()
        }
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun browsableItem(mediaId: String, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            baseMetadata(title, isBrowsable = true, isPlayable = false)
                .build(),
        )
        .build()

    private fun trackItem(track: Track): MediaItem {
        val uri = track.uri
        return MediaItem.Builder()
            .setMediaId(track.mediaId)
            .setUri(uri)
            .setMediaMetadata(
                baseMetadata(track.title, isBrowsable = false, isPlayable = true)
                    .setDisplayTitle(track.title)
                    .setSubtitle(track.displaySubtitle)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build(),
            )
            .build()
    }

    private fun baseMetadata(
        title: String,
        isBrowsable: Boolean,
        isPlayable: Boolean,
    ): MediaMetadata.Builder = MediaMetadata.Builder()
        .setTitle(title)
        .setIsBrowsable(isBrowsable)
        .setIsPlayable(isPlayable)

    private val Track.mediaId: String
        get() = "$TRACK_PREFIX$id"

    companion object {
        const val ROOT_ID = "root"
        private const val TRACK_PREFIX = "track:"
    }
}
