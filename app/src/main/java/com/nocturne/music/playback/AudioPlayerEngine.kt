package com.nocturne.music.playback

import android.content.Context
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.nocturne.music.core.model.PlaybackState
import com.nocturne.music.core.model.RepeatMode
import com.nocturne.music.core.model.ResolvedStream
import com.nocturne.music.core.model.Track
import com.nocturne.music.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
class AudioPlayerEngine(
    private val context: Context,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Track>>(emptyList())
    val currentQueue: StateFlow<List<Track>> = _currentQueue.asStateFlow()

    private var currentIndex = 0
    private var progressJob: Job? = null
    private var _currentTrack: Track? = null

    init {
        startProgressTracker()
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        _currentQueue.value = queue
        currentIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        loadAndPlay(track)
    }

    fun playQueue(queue: List<Track>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        _currentQueue.value = queue
        currentIndex = startIndex.coerceIn(0, queue.lastIndex)
        loadAndPlay(queue[currentIndex])
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _currentTrack != null) {
                loadAndPlay(_currentTrack!!)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(positionMs = positionMs)
    }

    fun next() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return
        if (currentIndex < queue.lastIndex) {
            currentIndex++
            loadAndPlay(queue[currentIndex])
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL) {
            currentIndex = 0
            loadAndPlay(queue[0])
        }
    }

    fun previous() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return
        if (exoPlayer.currentPosition > 3000L || currentIndex == 0) {
            exoPlayer.seekTo(0)
        } else {
            currentIndex--
            loadAndPlay(queue[currentIndex])
        }
    }

    fun toggleShuffle() {
        val current = _playbackState.value.shuffle
        _playbackState.value = _playbackState.value.copy(shuffle = !current)
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
        exoPlayer.repeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    private fun loadAndPlay(track: Track) {
        _currentTrack = track
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            isPlaying = true,
            queueIndex = currentIndex,
            queue = _currentQueue.value
        )

        scope.launch {
            val resolvedResult = musicRepository.resolveStream(track.id)
            if (resolvedResult.isSuccess) {
                val stream = resolvedResult.getOrThrow()
                playStream(track, stream)
            } else {
                // If stream resolution failed, advance to next track
                next()
            }
        }
    }

    private fun playStream(track: Track, stream: ResolvedStream) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artists)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.thumbnail?.let { android.net.Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(stream.url)
            .setMediaMetadata(mediaMetadata)
            .build()

        // Create matching HTTP data source for GoogleVideo streaming CDN
        val userAgent = stream.headers["User-Agent"]
            ?: "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)"

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(stream.headers)

        val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()

        // Apply loudness normalization gain if present
        stream.loudnessDb?.let { db ->
            val gainFactor = Math.pow(10.0, (-db / 20.0)).toFloat().coerceIn(0.1f, 1.5f)
            exoPlayer.volume = (_playbackState.value.volume * gainFactor).coerceIn(0f, 1f)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedMs = exoPlayer.bufferedPosition
                    )
                }
                Player.STATE_ENDED -> {
                    if (_playbackState.value.repeatMode == RepeatMode.ONE) {
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    } else {
                        next()
                    }
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Auto skip failed tracks
            next()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedMs = exoPlayer.bufferedPosition
                    )
                }
                delay(500)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        scope.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
