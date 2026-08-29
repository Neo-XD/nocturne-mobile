package com.nocturne.music.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RepeatMode {
    @SerialName("off")
    OFF,
    @SerialName("all")
    ALL,
    @SerialName("one")
    ONE
}

@Serializable
data class PlaybackState(
    @SerialName("current_track")
    val currentTrack: Track? = null,
    @SerialName("is_playing")
    val isPlaying: Boolean = false,
    @SerialName("position_ms")
    val positionMs: Long = 0L,
    @SerialName("duration_ms")
    val durationMs: Long = 0L,
    @SerialName("buffered_ms")
    val bufferedMs: Long = 0L,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val pitch: Int = 0,
    @SerialName("repeat_mode")
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val queue: List<Track> = emptyList(),
    @SerialName("queue_index")
    val queueIndex: Int = 0
)

@Serializable
data class ResolvedStream(
    @SerialName("video_id")
    val videoId: String,
    val url: String,
    val itag: Long,
    val headers: Map<String, String> = emptyMap(),
    @SerialName("expires_at")
    val expiresAt: Long = 0L,
    @SerialName("loudness_db")
    val loudnessDb: Double? = null,
    @SerialName("is_video")
    val isVideo: Boolean = false,
    @SerialName("stream_client")
    val streamClient: String = "WEB_REMIX"
)
