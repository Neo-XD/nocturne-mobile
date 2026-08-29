package com.music.vivi.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.vivi.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class RemoteConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class PlaybackDeviceTarget {
    LOCAL,
    REMOTE_DESKTOP
}

@Serializable
data class RemoteTrack(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val duration_ms: Long = 0,
    val queued_by: String? = null
)

@Serializable
data class RemoteRoomState(
    val room_code: String = "DIRECT",
    val host_id: String = "desktop",
    val current_track: RemoteTrack? = null,
    val is_playing: Boolean = false,
    val position_ms: Long = 0,
    val last_update_ms: Long = 0,
    val volume: Double = 1.0,
    val queue: List<RemoteTrack> = emptyList()
)

@Serializable
data class RemotePlaybackActionPayload(
    val kind: String,
    val position_ms: Long = 0,
    val track: RemoteTrack? = null,
    val playing: Boolean = false,
    val volume: Double = 1.0
)

@Serializable
data class RemoteWireMessage(
    val type: String,
    val client_device_name: String? = null,
    val pin_hash: String? = null,
    val success: Boolean? = null,
    val session_token: String? = null,
    val message: String? = null,
    val state: RemoteRoomState? = null,
    val action: RemotePlaybackActionPayload? = null
)

val RemoteSyncHostKey = stringPreferencesKey("remote_sync_host")
val RemoteSyncPortKey = intPreferencesKey("remote_sync_port")
val RemoteSyncPinKey = stringPreferencesKey("remote_sync_pin")
val RemoteSyncAutoConnectKey = booleanPreferencesKey("remote_sync_auto_connect")
val RemotePlaybackTargetKey = stringPreferencesKey("remote_playback_target")

@Singleton
class RemoteSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow(RemoteConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    private val _playbackTarget = MutableStateFlow(PlaybackDeviceTarget.LOCAL)
    val playbackTarget: StateFlow<PlaybackDeviceTarget> = _playbackTarget.asStateFlow()

    private val _remoteRoomState = MutableStateFlow<RemoteRoomState?>(null)
    val remoteRoomState: StateFlow<RemoteRoomState?> = _remoteRoomState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Disconnected")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val hostFlow = context.dataStore.data.map { it[RemoteSyncHostKey] ?: "192.168.1.10" }
    val portFlow = context.dataStore.data.map { it[RemoteSyncPortKey] ?: 8080 }
    val pinFlow = context.dataStore.data.map { it[RemoteSyncPinKey] ?: "1234" }

    init {
        scope.launch {
            context.dataStore.data.collectLatest { prefs ->
                val targetStr = prefs[RemotePlaybackTargetKey] ?: PlaybackDeviceTarget.LOCAL.name
                _playbackTarget.value = runCatching { PlaybackDeviceTarget.valueOf(targetStr) }
                    .getOrDefault(PlaybackDeviceTarget.LOCAL)

                val autoConnect = prefs[RemoteSyncAutoConnectKey] ?: false
                if (autoConnect && _connectionState.value == RemoteConnectionState.DISCONNECTED) {
                    val host = prefs[RemoteSyncHostKey] ?: "192.168.1.10"
                    val port = prefs[RemoteSyncPortKey] ?: 8080
                    val pin = prefs[RemoteSyncPinKey] ?: "1234"
                    connect(host, port, pin)
                }
            }
        }
    }

    fun setPlaybackTarget(target: PlaybackDeviceTarget) {
        _playbackTarget.value = target
        scope.launch {
            context.dataStore.edit { it[RemotePlaybackTargetKey] = target.name }
        }
    }

    fun connect(host: String, port: Int, pin: String) {
        if (_connectionState.value == RemoteConnectionState.CONNECTING || _connectionState.value == RemoteConnectionState.CONNECTED) {
            disconnect()
        }

        scope.launch {
            context.dataStore.edit {
                it[RemoteSyncHostKey] = host
                it[RemoteSyncPortKey] = port
                it[RemoteSyncPinKey] = pin
                it[RemoteSyncAutoConnectKey] = true
            }
        }

        _connectionState.value = RemoteConnectionState.CONNECTING
        _statusMessage.value = "Connecting to $host:$port..."

        val wsUrl = if (host.startsWith("ws://") || host.startsWith("wss://")) {
            host
        } else {
            "ws://$host:$port"
        }

        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Timber.d("RemoteSync: WebSocket open, sending AuthResponse with PIN")
                _statusMessage.value = "Authenticating..."
                val pinHash = sha256Hex(pin)
                val authMsg = RemoteWireMessage(
                    type = "auth_response",
                    client_device_name = "Nocturne Mobile (${android.os.Build.MODEL})",
                    pin_hash = pinHash
                )
                ws.send(json.encodeToString(authMsg))
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<RemoteWireMessage>(text)
                    when (msg.type) {
                        "auth_result" -> {
                            if (msg.success == true) {
                                _connectionState.value = RemoteConnectionState.CONNECTED
                                _statusMessage.value = "Connected to $host:$port"
                                Timber.d("RemoteSync: Authentication successful")
                            } else {
                                _connectionState.value = RemoteConnectionState.ERROR
                                _statusMessage.value = msg.message ?: "Authentication failed (Invalid PIN)"
                                ws.close(1000, "Auth failed")
                            }
                        }
                        "sync_state" -> {
                            msg.state?.let { newState ->
                                _remoteRoomState.value = newState
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "RemoteSync: Error parsing message $text")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "RemoteSync: WebSocket failure")
                _connectionState.value = RemoteConnectionState.ERROR
                _statusMessage.value = "Connection failed: ${t.localizedMessage ?: "Unreachable"}"
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Timber.d("RemoteSync: WebSocket closed ($code: $reason)")
                _connectionState.value = RemoteConnectionState.DISCONNECTED
                _statusMessage.value = "Disconnected"
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = RemoteConnectionState.DISCONNECTED
        _statusMessage.value = "Disconnected"
        scope.launch {
            context.dataStore.edit { it[RemoteSyncAutoConnectKey] = false }
        }
    }

    fun sendPlay() = sendAction(RemotePlaybackActionPayload(kind = "play", playing = true))
    fun sendPause() = sendAction(RemotePlaybackActionPayload(kind = "pause", playing = false))
    fun sendToggle() = sendAction(RemotePlaybackActionPayload(kind = "toggle"))
    fun sendNext() = sendAction(RemotePlaybackActionPayload(kind = "next_track"))
    fun sendPrevious() = sendAction(RemotePlaybackActionPayload(kind = "previous_track"))

    fun sendSeek(positionMs: Long) = sendAction(
        RemotePlaybackActionPayload(kind = "seek", position_ms = positionMs)
    )

    fun sendVolume(volume: Float) = sendAction(
        RemotePlaybackActionPayload(kind = "set_volume", volume = volume.toDouble())
    )

    fun sendChangeTrack(track: RemoteTrack) = sendAction(
        RemotePlaybackActionPayload(kind = "change_track", track = track)
    )

    private fun sendAction(action: RemotePlaybackActionPayload) {
        val ws = webSocket
        if (ws != null && _connectionState.value == RemoteConnectionState.CONNECTED) {
            val wireMsg = RemoteWireMessage(
                type = "playback_action",
                action = action
            )
            ws.send(json.encodeToString(wireMsg))
        }
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
