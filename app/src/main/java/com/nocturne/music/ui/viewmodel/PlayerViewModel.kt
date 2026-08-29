package com.nocturne.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.nocturne.music.core.model.Lyrics
import com.nocturne.music.core.model.PlaybackState
import com.nocturne.music.core.model.Track
import com.nocturne.music.data.repository.MusicRepository
import com.nocturne.music.playback.AudioPlayerEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@UnstableApi
class PlayerViewModel(
    private val audioPlayerEngine: AudioPlayerEngine,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = audioPlayerEngine.playbackState

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _isPlayerSheetVisible = MutableStateFlow(false)
    val isPlayerSheetVisible: StateFlow<Boolean> = _isPlayerSheetVisible.asStateFlow()

    init {
        viewModelScope.launch {
            playbackState.map { it.currentTrack }.distinctUntilChanged().collect { track ->
                if (track != null) {
                    loadLyrics(track)
                } else {
                    _lyrics.value = null
                }
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        audioPlayerEngine.playTrack(track, queue)
    }

    fun togglePlayPause() {
        audioPlayerEngine.togglePlayPause()
    }

    fun next() {
        audioPlayerEngine.next()
    }

    fun previous() {
        audioPlayerEngine.previous()
    }

    fun seekTo(positionMs: Long) {
        audioPlayerEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        audioPlayerEngine.toggleShuffle()
    }

    fun cycleRepeatMode() {
        audioPlayerEngine.cycleRepeatMode()
    }

    fun showPlayerSheet() {
        _isPlayerSheetVisible.value = true
    }

    fun hidePlayerSheet() {
        _isPlayerSheetVisible.value = false
    }

    private fun loadLyrics(track: Track) {
        viewModelScope.launch {
            val res = musicRepository.getLyrics(track.id, track.title, track.artists)
            _lyrics.value = res
        }
    }
}
