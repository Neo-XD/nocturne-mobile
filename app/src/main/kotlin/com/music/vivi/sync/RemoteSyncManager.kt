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
    val success: Boolean? = null,
    val session_token: String? = null,
    val message: String? = null,
    val state: RemoteRoomState? = null,
    val action: RemotePlaybackActionPayload? = null
)

val RemoteSyncHostKey = stringPreferencesKey("remote_sync_host")
val RemoteSyncPortKey = intPreferencesKey("remote_sync_port")
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
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null

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
                    connect(host, port)
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

    fun connect(host: String, port: Int = 8080) {
        reconnectJob?.cancel()
        disconnectInternal(clearAutoConnect = false)

        val cleanHost = host.trim()
        scope.launch {
            context.dataStore.edit {
                it[RemoteSyncHostKey] = cleanHost
                it[RemoteSyncPortKey] = port
                it[RemoteSyncAutoConnectKey] = true
            }
        }

        _connectionState.value = RemoteConnectionState.CONNECTING
        _statusMessage.value = "Connecting to $cleanHost:$port..."

        val wsUrl = if (cleanHost.startsWith("ws://") || cleanHost.startsWith("wss://")) {
            cleanHost
        } else {
            "ws://$cleanHost:$port"
        }

        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("RemoteSync: Connected to $cleanHost:$port")
                _connectionState.value = RemoteConnectionState.CONNECTED
                _statusMessage.value = "Connected to $cleanHost:$port"
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<RemoteWireMessage>(text)
                    when (msg.type) {
                        "auth_result" -> {
                            _connectionState.value = RemoteConnectionState.CONNECTED
                            _statusMessage.value = "Connected to $cleanHost:$port"
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

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "RemoteSync: Connection failure")
                _connectionState.value = RemoteConnectionState.ERROR
                _statusMessage.value = "Connection error: ${t.localizedMessage ?: "Unreachable"}"
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("RemoteSync: Closed ($code: $reason)")
                _connectionState.value = RemoteConnectionState.DISCONNECTED
                _statusMessage.value = "Disconnected"
            }
        })
    }

    fun disconnect() {
        disconnectInternal(clearAutoConnect = true)
    }

    private fun disconnectInternal(clearAutoConnect: Boolean) {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = RemoteConnectionState.DISCONNECTED
        _statusMessage.value = "Disconnected"
        if (clearAutoConnect) {
            scope.launch {
                context.dataStore.edit { it[RemoteSyncAutoConnectKey] = false }
            }
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
}
