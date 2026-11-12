package com.nocturne.music.sync

import android.content.Context
import android.net.wifi.WifiManager
import com.nocturne.music.data.model.Song
import com.nocturne.music.data.model.artworkAt
import com.nocturne.music.data.model.durationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

enum class NocturneSyncState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED_BLOCKED
}

enum class NocturnePlaybackTarget {
    LOCAL,
    REMOTE_PC
}

@Serializable
data class SyncRemoteTrack(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val duration_ms: Long = 0,
)

@Serializable
data class SyncRemoteRoomState(
    val room_code: String = "DIRECT",
    val host_id: String = "desktop",
    val current_track: SyncRemoteTrack? = null,
    val is_playing: Boolean = false,
    val position_ms: Long = 0,
    val last_update_ms: Long = 0,
    val volume: Double = 1.0,
    val queue: List<SyncRemoteTrack> = emptyList(),
)

@Serializable
data class SyncPlaybackActionPayload(
    val kind: String,
    val position_ms: Long = 0,
    val track: SyncRemoteTrack? = null,
    val playing: Boolean = false,
    val volume: Double = 1.0,
    val from_index: Int = 0,
    val to_index: Int = 0,
    val queue: List<SyncRemoteTrack>? = null,
)

@Serializable
data class SyncWireMessage(
    val type: String,
    val client_device_name: String? = null,
    val pin_hash: String? = null,
    val nonce: String? = null,
    val host_device_name: String? = null,
    val success: Boolean? = null,
    val session_token: String? = null,
    val message: String? = null,
    val state: SyncRemoteRoomState? = null,
    val action: SyncPlaybackActionPayload? = null,
)

@Serializable
data class DiscoveryBeacon(
    val service: String? = null,
    val device_name: String? = null,
    val port: Int? = null,
    val ip: String? = null,
)

@Serializable
data class SavedSyncDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8080,
    val pin: String = "",
    val sessionToken: String? = null,
    val lastConnectedMs: Long = 0L,
)

object NocturneSyncManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private var activeSocket: WebSocket? = null
    private var connectJob: Job? = null
    private var discoveryJob: Job? = null

    val connectionState = MutableStateFlow(NocturneSyncState.DISCONNECTED)
    val statusMessage = MutableStateFlow("Disconnected")
    val playbackTarget = MutableStateFlow(NocturnePlaybackTarget.LOCAL)
    val roomState = MutableStateFlow<SyncRemoteRoomState?>(null)
    val sessionToken = MutableStateFlow<String?>(null)
    val discoveredDevice = MutableStateFlow<String?>(null)
    val discoveredIp = MutableStateFlow<String?>(null)
    val discoveredPort = MutableStateFlow(8080)

    val savedDevices = MutableStateFlow<List<SavedSyncDevice>>(emptyList())
    val activeConnectedDevice = MutableStateFlow<SavedSyncDevice?>(null)

    // Current connection target (empty by default so fields remain blank for next pairing)
    val host = MutableStateFlow("")
    val port = MutableStateFlow(8080)
    val pin = MutableStateFlow("")

    private var appContext: Context? = null
    private var lastDiscoveredHostName: String = ""

    fun init(context: Context) {
        appContext = context.applicationContext
        val sp = context.getSharedPreferences("nocturne_sync", Context.MODE_PRIVATE)
        val rawSaved = sp.getString("saved_devices_v2", null)
        if (!rawSaved.isNullOrEmpty()) {
            runCatching {
                savedDevices.value = json.decodeFromString<List<SavedSyncDevice>>(rawSaved)
            }
        }
        sessionToken.value = sp.getString("token", null)

        startLanDiscovery()

        // Attempt auto-connect to the most recent saved device if available
        val mostRecent = savedDevices.value.maxByOrNull { it.lastConnectedMs }
        if (mostRecent != null) {
            scope.launch {
                delay(1200)
                if (connectionState.value == NocturneSyncState.DISCONNECTED) {
                    startConnect(
                        targetHost = mostRecent.host,
                        targetPort = mostRecent.port,
                        targetPin = mostRecent.pin,
                        targetToken = mostRecent.sessionToken,
                        expectedName = mostRecent.name
                    )
                }
            }
        }
    }

    private fun persistSavedDevices(list: List<SavedSyncDevice>) {
        savedDevices.value = list
        appContext?.getSharedPreferences("nocturne_sync", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("saved_devices_v2", json.encodeToString(list))
            apply()
        }
    }

    fun removeSavedDevice(id: String) {
        val updated = savedDevices.value.filterNot { it.id == id }
        persistSavedDevices(updated)
        if (activeConnectedDevice.value?.id == id) {
            disconnect()
        }
    }

    fun connectToSaved(device: SavedSyncDevice) {
        startConnect(
            targetHost = device.host,
            targetPort = device.port,
            targetPin = device.pin,
            targetToken = device.sessionToken,
            expectedName = device.name
        )
    }

    fun startLanDiscovery() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = scope.launch {
            val wifiManager = runCatching {
                appContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            }.getOrNull()

            val multicastLock = runCatching {
                wifiManager?.createMulticastLock("nocturne_sync_discovery")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
            }.getOrNull()

            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 2500
                val buffer = ByteArray(2048)

                while (isActive) {
                    try {
                        val pingBytes = """{"service":"nocturne-discover"}""".toByteArray(Charsets.UTF_8)
                        val broadcastAddr = InetAddress.getByName("255.255.255.255")
                        val pingPacket = DatagramPacket(pingBytes, pingBytes.size, broadcastAddr, 8081)
                        socket.send(pingPacket)
                    } catch (_: Exception) {}

                    val deadline = System.currentTimeMillis() + 3500
                    while (System.currentTimeMillis() < deadline && isActive) {
                        try {
                            val inPacket = DatagramPacket(buffer, buffer.size)
                            socket.receive(inPacket)
                            val text = String(inPacket.data, 0, inPacket.length, Charsets.UTF_8)
                            if (text.contains("nocturne-sync")) {
                                val obj = json.decodeFromString<DiscoveryBeacon>(text)
                                val devName = obj.device_name ?: "Nocturne PC"
                                val devIp = obj.ip ?: inPacket.address?.hostAddress ?: ""
                                val devPort = obj.port ?: 8080
                                if (devIp.isNotBlank()) {
                                    lastDiscoveredHostName = devName
                                    discoveredDevice.value = "$devName ($devIp)"
                                    discoveredIp.value = devIp
                                    discoveredPort.value = devPort

                                    // If auto-connectable to a saved device matching this IP
                                    if (connectionState.value == NocturneSyncState.DISCONNECTED) {
                                        val match = savedDevices.value.find { it.host == devIp }
                                        if (match != null) {
                                            startConnect(
                                                targetHost = match.host,
                                                targetPort = match.port,
                                                targetPin = match.pin,
                                                targetToken = match.sessionToken,
                                                expectedName = match.name
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (_: SocketTimeoutException) {
                            break
                        } catch (_: Exception) {}
                    }
                    delay(4000)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { socket?.close() }
                runCatching { multicastLock?.release() }
            }
        }
    }

    /**
     * Start connection with anti-spam limiter:
     * Sends at most 2 requests spaced by 3 seconds.
     * If both fail, stops completely before the phone gets blocked.
     */
    fun startConnect(
        targetHost: String = host.value,
        targetPort: Int = port.value,
        targetPin: String = pin.value,
        targetToken: String? = null,
        expectedName: String? = null,
    ) {
        val cleanHost = targetHost.trim()
        if (cleanHost.isBlank()) {
            statusMessage.value = "Enter PC IP address"
            return
        }

        connectJob?.cancel()
        disconnectInternal()
        connectionState.value = NocturneSyncState.CONNECTING
        statusMessage.value = "Connecting to $cleanHost:$targetPort (Attempt 1/2)..."

        connectJob = scope.launch {
            // Attempt 1
            val ok1 = tryConnectOnce(cleanHost, targetPort, targetPin, targetToken, expectedName)
            if (ok1) return@launch

            // Gap of 3 seconds between requests
            statusMessage.value = "Attempt 1 failed. Waiting 3s before retry..."
            delay(3000)

            // Attempt 2
            statusMessage.value = "Retrying connection to $cleanHost:$targetPort (Attempt 2/2)..."
            val ok2 = tryConnectOnce(cleanHost, targetPort, targetPin, targetToken, expectedName)
            if (ok2) return@launch

            // Both attempts failed: STOP to avoid spamming and blocking
            connectionState.value = NocturneSyncState.FAILED_BLOCKED
            statusMessage.value = "Connection failed after 2 attempts. Stopped to avoid device block. Check PC IP/OTP and tap Connect to retry."
            disconnectInternal()
        }
    }

    private suspend fun tryConnectOnce(
        h: String,
        p: Int,
        userPin: String,
        token: String? = null,
        expectedName: String? = null,
    ): Boolean =
        suspendCancellableCoroutine { cont ->
            val cleanHost = h.trim()
            val wsUrl = if (cleanHost.startsWith("ws://") || cleanHost.startsWith("wss://")) {
                cleanHost
            } else {
                "ws://$cleanHost:$p"
            }

            val request = try {
                Request.Builder().url(wsUrl).build()
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(false)
                return@suspendCancellableCoroutine
            }

            var resumed = false
            fun safeResume(success: Boolean) {
                if (!resumed && cont.isActive) {
                    resumed = true
                    cont.resume(success)
                }
            }

            var detectedHostName: String = expectedName ?: lastDiscoveredHostName.ifEmpty { "Nocturne PC" }

            val socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    statusMessage.value = "Handshaking with PC..."
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val msg = json.decodeFromString<SyncWireMessage>(text)
                        when (msg.type) {
                            "auth_challenge" -> {
                                msg.host_device_name?.let { if (it.isNotBlank()) detectedHostName = it }
                                val nonce = msg.nonce.orEmpty()
                                val tokenToSend = token ?: sessionToken.value
                                val trimmed = userPin.trim()
                                val pinHashToSend = if (trimmed.isNotEmpty()) pinProof(nonce, trimmed) else null
                                val responseMsg = SyncWireMessage(
                                    type = "auth_response",
                                    client_device_name = "Nocturne Mobile (${android.os.Build.MODEL})",
                                    pin_hash = pinHashToSend,
                                    session_token = tokenToSend,
                                )
                                webSocket.send(json.encodeToString(responseMsg))
                            }
                            "auth_result" -> {
                                if (msg.success == true) {
                                    activeSocket = webSocket
                                    connectionState.value = NocturneSyncState.CONNECTED
                                    statusMessage.value = "Connected to $detectedHostName"

                                    val finalToken = msg.session_token ?: token ?: sessionToken.value
                                    if (finalToken != null) {
                                        sessionToken.value = finalToken
                                        appContext?.getSharedPreferences("nocturne_sync", Context.MODE_PRIVATE)
                                            ?.edit()?.putString("token", finalToken)?.apply()
                                    }

                                    // Save connected device so user can connect anytime without re-entering
                                    val savedDev = SavedSyncDevice(
                                        id = "$cleanHost:$p",
                                        name = detectedHostName,
                                        host = cleanHost,
                                        port = p,
                                        pin = userPin.trim(),
                                        sessionToken = finalToken,
                                        lastConnectedMs = System.currentTimeMillis()
                                    )
                                    activeConnectedDevice.value = savedDev
                                    val updatedList = savedDevices.value.filterNot { it.id == savedDev.id } + savedDev
                                    persistSavedDevices(updatedList)

                                    // Once connected, phone app queries PC for current playback state
                                    webSocket.send(json.encodeToString(
                                        SyncWireMessage(
                                            type = "playback_action",
                                            action = SyncPlaybackActionPayload(kind = "query_state")
                                        )
                                    ))

                                    safeResume(true)
                                } else {
                                    statusMessage.value = msg.message ?: "Pairing rejected by PC"
                                    webSocket.close(1000, "auth failed")
                                    safeResume(false)
                                }
                            }
                            "sync_state" -> {
                                val s = msg.state
                                val track = s?.current_track
                                if (track == null || track.id.isBlank() || track.title.isBlank()) {
                                    // PC says no song: nothing gets shown on phone
                                    roomState.value = null
                                } else {
                                    roomState.value = s
                                }
                            }
                        }
                    } catch (_: Exception) {
                        safeResume(false)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    safeResume(false)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (activeSocket == webSocket) {
                        activeSocket = null
                        activeConnectedDevice.value = null
                        roomState.value = null
                        if (connectionState.value == NocturneSyncState.CONNECTED) {
                            connectionState.value = NocturneSyncState.DISCONNECTED
                            statusMessage.value = "Disconnected"
                        }
                    }
                    safeResume(false)
                }
            })

            cont.invokeOnCancellation {
                socket.cancel()
            }
        }

    fun disconnect() {
        connectJob?.cancel()
        disconnectInternal()
        connectionState.value = NocturneSyncState.DISCONNECTED
        statusMessage.value = "Disconnected"
        activeConnectedDevice.value = null
        roomState.value = null
    }

    private fun disconnectInternal() {
        activeSocket?.close(1000, "User disconnected")
        activeSocket = null
        activeConnectedDevice.value = null
    }

    fun setPlaybackTarget(target: NocturnePlaybackTarget) {
        playbackTarget.value = target
    }

    fun sendAction(action: SyncPlaybackActionPayload) {
        val ws = activeSocket
        if (ws != null && connectionState.value == NocturneSyncState.CONNECTED) {
            val wireMsg = SyncWireMessage(type = "playback_action", action = action)
            ws.send(json.encodeToString(wireMsg))
        }
    }

    /**
     * Called when a song is played on phone:
     * If no song is played on PC and devices are connected,
     * PC receives the timestamp, track details, queue, and playback state!
     */
    fun onLocalSongPlayed(
        song: Song,
        positionMs: Long,
        isPlaying: Boolean,
        queue: List<Song>
    ) {
        if (connectionState.value != NocturneSyncState.CONNECTED) return
        val pcHasSong = roomState.value?.let { it.current_track != null && it.is_playing } == true
        if (!pcHasSong) {
            val remoteQueue = queue.map { q ->
                SyncRemoteTrack(
                    id = q.videoId,
                    title = q.title,
                    artist = q.artists.joinToString { it.name },
                    thumbnail = q.artworkAt(400) ?: q.thumbnailUrl,
                    duration_ms = q.durationMillis() ?: 0L
                )
            }
            val currentRemote = SyncRemoteTrack(
                id = song.videoId,
                title = song.title,
                artist = song.artists.joinToString { it.name },
                thumbnail = song.artworkAt(400) ?: song.thumbnailUrl,
                duration_ms = song.durationMillis() ?: 0L
            )
            sendAction(
                SyncPlaybackActionPayload(
                    kind = "sync_queue_from_mobile",
                    track = currentRemote,
                    position_ms = positionMs,
                    playing = isPlaying,
                    queue = remoteQueue.ifEmpty { listOf(currentRemote) }
                )
            )
        }
    }

    fun sendPlay() = sendAction(SyncPlaybackActionPayload(kind = "play", playing = true))
    fun sendPause() = sendAction(SyncPlaybackActionPayload(kind = "pause", playing = false))
    fun sendToggle() = sendAction(SyncPlaybackActionPayload(kind = "toggle"))
    fun sendNext() = sendAction(SyncPlaybackActionPayload(kind = "next_track"))
    fun sendPrevious() = sendAction(SyncPlaybackActionPayload(kind = "previous_track"))
    fun sendSeek(positionMs: Long) = sendAction(SyncPlaybackActionPayload(kind = "seek", position_ms = positionMs))
}
