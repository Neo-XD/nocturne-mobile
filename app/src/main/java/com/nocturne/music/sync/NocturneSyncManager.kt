package com.nocturne.music.sync

import android.content.Context
import android.net.wifi.WifiManager
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

    val host = MutableStateFlow("192.168.1.10")
    val port = MutableStateFlow(8080)
    val pin = MutableStateFlow("")

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val sp = context.getSharedPreferences("nocturne_sync", Context.MODE_PRIVATE)
        host.value = sp.getString("host", "192.168.1.10") ?: "192.168.1.10"
        port.value = sp.getInt("port", 8080)
        pin.value = sp.getString("pin", "") ?: ""
        sessionToken.value = sp.getString("token", null)
        startLanDiscovery()
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
                                    discoveredDevice.value = "$devName ($devIp)"
                                    if (host.value == "192.168.1.10" || host.value.isBlank()) {
                                        host.value = devIp
                                        port.value = devPort
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

    fun saveConfig(newHost: String, newPort: Int, newPin: String) {
        host.value = newHost.trim()
        port.value = newPort
        pin.value = newPin.trim()
        appContext?.getSharedPreferences("nocturne_sync", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("host", host.value)
            putInt("port", port.value)
            putString("pin", pin.value)
            apply()
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
    ) {
        connectJob?.cancel()
        disconnectInternal()
        connectionState.value = NocturneSyncState.CONNECTING
        statusMessage.value = "Connecting to $targetHost:$targetPort (Attempt 1/2)..."

        connectJob = scope.launch {
            // Attempt 1
            val ok1 = tryConnectOnce(targetHost, targetPort, targetPin)
            if (ok1) return@launch

            // Gap of 3 seconds between requests
            statusMessage.value = "Attempt 1 failed. Waiting 3s before retry..."
            delay(3000)

            // Attempt 2
            statusMessage.value = "Retrying connection to $targetHost:$targetPort (Attempt 2/2)..."
            val ok2 = tryConnectOnce(targetHost, targetPort, targetPin)
            if (ok2) return@launch

            // Both attempts failed: STOP to avoid spamming and blocking
            connectionState.value = NocturneSyncState.FAILED_BLOCKED
            statusMessage.value = "Connection failed after 2 attempts. Stopped to avoid device block. Check PC IP/OTP and tap Connect to retry."
            disconnectInternal()
        }
    }

    private suspend fun tryConnectOnce(h: String, p: Int, userPin: String): Boolean =
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

            val socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    statusMessage.value = "Handshaking with PC..."
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val msg = json.decodeFromString<SyncWireMessage>(text)
                        when (msg.type) {
                            "auth_challenge" -> {
                                val nonce = msg.nonce.orEmpty()
                                val tokenToSend = sessionToken.value
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
                                    statusMessage.value = "Connected to Nocturne PC"
                                    msg.session_token?.let { tok ->
                                        sessionToken.value = tok
                                        appContext?.getSharedPreferences("nocturne_sync", Context.MODE_PRIVATE)
                                            ?.edit()?.putString("token", tok)?.apply()
                                    }
                                    safeResume(true)
                                } else {
                                    statusMessage.value = msg.message ?: "Pairing rejected by PC"
                                    webSocket.close(1000, "auth failed")
                                    safeResume(false)
                                }
                            }
                            "sync_state" -> {
                                msg.state?.let { roomState.value = it }
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
    }

    private fun disconnectInternal() {
        activeSocket?.close(1000, "User disconnected")
        activeSocket = null
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

    fun sendPlay() = sendAction(SyncPlaybackActionPayload(kind = "play", playing = true))
    fun sendPause() = sendAction(SyncPlaybackActionPayload(kind = "pause", playing = false))
    fun sendToggle() = sendAction(SyncPlaybackActionPayload(kind = "toggle"))
    fun sendNext() = sendAction(SyncPlaybackActionPayload(kind = "next_track"))
    fun sendPrevious() = sendAction(SyncPlaybackActionPayload(kind = "previous_track"))
    fun sendSeek(positionMs: Long) = sendAction(SyncPlaybackActionPayload(kind = "seek", position_ms = positionMs))
}
