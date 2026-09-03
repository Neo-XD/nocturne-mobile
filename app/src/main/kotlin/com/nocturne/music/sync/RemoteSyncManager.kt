package com.nocturne.music.sync

import android.content.Context
import android.net.wifi.WifiManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nocturne.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.runtime.staticCompositionLocalOf

val LocalRemoteSyncManager = staticCompositionLocalOf<RemoteSyncManager> {
    error("No RemoteSyncManager provided")
}

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
    val nonce: String? = null,
    val host_device_name: String? = null,
    val success: Boolean? = null,
    val session_token: String? = null,
    val message: String? = null,
    val state: RemoteRoomState? = null,
    val action: RemotePlaybackActionPayload? = null
)

@Serializable
data class DiscoveredDevice(
    val ip: String,
    val port: Int = 8080,
    val name: String = "Nocturne PC",
    val lastSeenMs: Long = System.currentTimeMillis()
)

@Serializable
private data class DiscoveryPayload(
    val service: String? = null,
    val device_name: String? = null,
    val port: Int? = null,
    val ip: String? = null
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
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var discoveryJob: Job? = null

    private val _connectionState = MutableStateFlow(RemoteConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    private val _playbackTarget = MutableStateFlow(PlaybackDeviceTarget.LOCAL)
    val playbackTarget: StateFlow<PlaybackDeviceTarget> = _playbackTarget.asStateFlow()

    private val _remoteRoomState = MutableStateFlow<RemoteRoomState?>(null)
    val remoteRoomState: StateFlow<RemoteRoomState?> = _remoteRoomState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Disconnected")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    val hostFlow = context.dataStore.data.map { it[RemoteSyncHostKey] ?: "192.168.1.10" }
    val portFlow = context.dataStore.data.map { it[RemoteSyncPortKey] ?: 8080 }
    val pinFlow = context.dataStore.data.map { it[RemoteSyncPinKey].orEmpty() }

    init {
        startLanDiscovery()

        scope.launch {
            context.dataStore.data.collectLatest { prefs ->
                val targetStr = prefs[RemotePlaybackTargetKey] ?: PlaybackDeviceTarget.LOCAL.name
                _playbackTarget.value = runCatching { PlaybackDeviceTarget.valueOf(targetStr) }
                    .getOrDefault(PlaybackDeviceTarget.LOCAL)

                val autoConnect = prefs[RemoteSyncAutoConnectKey] ?: false
                if (autoConnect && _connectionState.value == RemoteConnectionState.DISCONNECTED) {
                    val host = prefs[RemoteSyncHostKey] ?: "192.168.1.10"
                    val port = prefs[RemoteSyncPortKey] ?: 8080

                    connect(host, port, prefs[RemoteSyncPinKey].orEmpty())
                }
            }
        }
    }

    fun startLanDiscovery() {
        if (discoveryJob?.isActive == true) return

        discoveryJob = scope.launch {
            val wifiManager = runCatching {
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            }.getOrNull()

            val multicastLock = runCatching {
                wifiManager?.createMulticastLock("nocturne_discovery")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
            }.getOrNull()

            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 2000

                val buffer = ByteArray(2048)

                while (isActive) {
                    // Send discovery ping to broadcast
                    try {
                        val pingData = "{\"type\":\"nocturne-discover\"}".toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(
                            pingData,
                            pingData.size,
                            InetAddress.getByName("255.255.255.255"),
                            8081
                        )
                        socket.send(packet)
                    } catch (e: Exception) {
                        Timber.d("Discovery ping send failed: ${e.message}")
                    }

                    // Receive responses & beacons
                    val endTime = System.currentTimeMillis() + 2500
                    while (System.currentTimeMillis() < endTime && isActive) {
                        try {
                            val receivePacket = DatagramPacket(buffer, buffer.size)
                            socket.receive(receivePacket)
                            val text = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                            val payload = runCatching { json.decodeFromString<DiscoveryPayload>(text) }.getOrNull()

                            if (payload?.service == "nocturne-sync") {
                                val senderIp = payload.ip ?: receivePacket.address.hostAddress ?: ""
                                val port = payload.port ?: 8080
                                val name = payload.device_name ?: "Nocturne PC"

                                if (senderIp.isNotEmpty() && !senderIp.startsWith("127.")) {
                                    val newDevice = DiscoveredDevice(
                                        ip = senderIp,
                                        port = port,
                                        name = name,
                                        lastSeenMs = System.currentTimeMillis()
                                    )
                                    val current = _discoveredDevices.value.filter {
                                        it.ip != senderIp && (System.currentTimeMillis() - it.lastSeenMs < 15000)
                                    }
                                    _discoveredDevices.value = current + newDevice
                                }
                            }
                        } catch (_: Exception) {
                            // Timeout is normal in receive loop
                        }
                    }

                    delay(3000)
                }
            } catch (e: Exception) {
                Timber.e(e, "Discovery engine exception")
            } finally {
                socket?.close()
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
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

    fun connect(host: String, port: Int = 8080, pin: String) {
        disconnectInternal(clearAutoConnect = false)

        // The desktop only accepts 4 to 8 digits, so sending anything else spends a lockout slot on a guess already known to be wrong.
        if (pin.length !in 4..8 || !pin.all { it.isDigit() }) {
            _connectionState.value = RemoteConnectionState.ERROR
            _statusMessage.value = "Enter the desktop's 4-8 digit pairing PIN in Remote Sync settings first"
            return
        }

        var authRejected = false
        val cleanHost = host.trim()
        scope.launch {
            context.dataStore.edit {
                it[RemoteSyncHostKey] = cleanHost
                it[RemoteSyncPortKey] = port
                it[RemoteSyncPinKey] = pin
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
                _statusMessage.value = "Pairing with $cleanHost:$port"
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<RemoteWireMessage>(text)
                    when (msg.type) {
                        "auth_challenge" -> {
                            val nonce = msg.nonce.orEmpty()
                            webSocket.send(
                                json.encodeToString(
                                    RemoteWireMessage(
                                        type = "auth_response",
                                        client_device_name = "Nocturne Mobile (${android.os.Build.MODEL})",
                                        pin_hash = pinProof(nonce, pin)
                                    )
                                )
                            )
                        }
                        "auth_result" -> {
                            if (msg.success == true) {
                                _connectionState.value = RemoteConnectionState.CONNECTED
                                _statusMessage.value = "Connected to $cleanHost:$port"
                                scope.launch {
                                    context.dataStore.edit { it[RemoteSyncAutoConnectKey] = true }
                                }
                            } else {
                                authRejected = true
                                _connectionState.value = RemoteConnectionState.ERROR
                                _statusMessage.value = msg.message ?: "Pairing rejected"
                                webSocket.close(1000, "auth failed")
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

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "RemoteSync: Connection failure")
                _connectionState.value = RemoteConnectionState.ERROR
                _statusMessage.value = "Connection error: ${t.localizedMessage ?: "Unreachable"}"
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("RemoteSync: Closed ($code: $reason)")
                if (authRejected) return
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

    fun sendAction(action: RemotePlaybackActionPayload) {
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
