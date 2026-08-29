package com.nocturne.music.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PlaybackKind {
    @SerialName("play")
    PLAY,
    @SerialName("pause")
    PAUSE,
    @SerialName("seek")
    SEEK,
    @SerialName("change_track")
    CHANGE_TRACK,
    @SerialName("sync_queue")
    SYNC_QUEUE,
    @SerialName("set_volume")
    SET_VOLUME
}

@Serializable
data class SyncPlayback(
    val kind: PlaybackKind,
    @SerialName("position_ms")
    val positionMs: Long = 0L,
    @SerialName("server_time_ms")
    val serverTimeMs: Long = 0L,
    val track: Track? = null,
    val queue: List<Track>? = null,
    val playing: Boolean = false,
    val volume: Double = 1.0
)

@Serializable
data class SyncUser(
    @SerialName("user_id")
    val userId: String,
    val username: String,
    @SerialName("is_host")
    val isHost: Boolean,
    @SerialName("is_connected")
    val isConnected: Boolean
)

@Serializable
data class SyncRoomState(
    @SerialName("room_code")
    val roomCode: String = "",
    @SerialName("host_id")
    val hostId: String = "",
    val users: List<SyncUser> = emptyList(),
    @SerialName("current_track")
    val currentTrack: Track? = null,
    @SerialName("is_playing")
    val isPlaying: Boolean = false,
    @SerialName("position_ms")
    val positionMs: Long = 0L,
    @SerialName("last_update_ms")
    val lastUpdateMs: Long = 0L,
    val volume: Double = 1.0,
    val queue: List<Track> = emptyList()
) {
    fun livePositionMs(nowMs: Long): Long {
        var p = positionMs
        if (isPlaying && lastUpdateMs > 0) {
            val elapsed = nowMs - lastUpdateMs
            if (elapsed > 0) p += elapsed
        }
        return p.coerceAtLeast(0L)
    }
}

/**
 * Protocol messages for Direct IP / Tailscale Sync with PIN Authentication
 */
@Serializable
sealed class SyncMessage {
    @Serializable
    @SerialName("auth_challenge")
    data class AuthChallenge(val nonce: String, val hostDeviceName: String) : SyncMessage()

    @Serializable
    @SerialName("auth_response")
    data class AuthResponse(val clientDeviceName: String, val pinHash: String) : SyncMessage()

    @Serializable
    @SerialName("auth_result")
    data class AuthResult(val success: Boolean, val sessionToken: String? = null, val message: String? = null) : SyncMessage()

    @Serializable
    @SerialName("sync_state")
    data class StateUpdate(val state: SyncRoomState) : SyncMessage()

    @Serializable
    @SerialName("playback_action")
    data class Action(val action: SyncPlayback) : SyncMessage()

    @Serializable
    @SerialName("ping")
    data object Ping : SyncMessage()

    @Serializable
    @SerialName("pong")
    data object Pong : SyncMessage()
}
