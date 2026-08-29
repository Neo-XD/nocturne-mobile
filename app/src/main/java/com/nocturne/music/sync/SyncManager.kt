package com.nocturne.music.sync

import com.nocturne.music.core.model.*
import com.nocturne.music.playback.AudioPlayerEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.security.MessageDigest

enum class SyncConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    ERROR
}

class SyncManager(
    private val httpClient: HttpClient,
    private val json: Json,
    private val audioPlayerEngine: AudioPlayerEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _status = MutableStateFlow(SyncConnectionStatus.DISCONNECTED)
    val status: StateFlow<SyncConnectionStatus> = _status.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _connectedHost = MutableStateFlow<String?>(null)
    val connectedHost: StateFlow<String?> = _connectedHost.asStateFlow()

    private var clientJob: Job? = null
    private var outgoingChannel: (suspend (String) -> Unit)? = null

    fun connectToHost(ipOrHost: String, port: Int = 8080, pin: String) {
        disconnect()
        _status.value = SyncConnectionStatus.CONNECTING
        _errorMessage.value = null

        clientJob = scope.launch {
            try {
                val hostUrl = if (ipOrHost.startsWith("ws://") || ipOrHost.startsWith("wss://")) ipOrHost else "ws://$ipOrHost:$port"
                httpClient.webSocket(hostUrl) {
                    _status.value = SyncConnectionStatus.AUTHENTICATING
                    outgoingChannel = { msg -> send(Frame.Text(msg)) }

                    // Send authentication with PIN hash
                    val pinHash = hashPin(pin)
                    val authMsg = json.encodeToString(
                        SyncMessage.serializer(),
                        SyncMessage.AuthResponse(
                            clientDeviceName = android.os.Build.MODEL ?: "Nocturne Mobile",
                            pinHash = pinHash
                        )
                    )
                    send(Frame.Text(authMsg))

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            handleIncomingMessage(text)
                        }
                    }
                }
            } catch (e: Exception) {
                _status.value = SyncConnectionStatus.ERROR
                _errorMessage.value = e.localizedMessage ?: "Failed to connect to host"
            } finally {
                _status.value = SyncConnectionStatus.DISCONNECTED
                outgoingChannel = null
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val message = json.decodeFromString(SyncMessage.serializer(), text)
            when (message) {
                is SyncMessage.AuthResult -> {
                    if (message.success) {
                        _status.value = SyncConnectionStatus.CONNECTED
                    } else {
                        _status.value = SyncConnectionStatus.ERROR
                        _errorMessage.value = message.message ?: "Invalid PIN"
                        disconnect()
                    }
                }
                is SyncMessage.StateUpdate -> {
                    val state = message.state
                    state.currentTrack?.let { track ->
                        // Apply remote track and playback state
                        if (audioPlayerEngine.playbackState.value.currentTrack?.id != track.id) {
                            audioPlayerEngine.playTrack(track, state.queue.ifEmpty { listOf(track) })
                        }
                    }
                }
                is SyncMessage.Action -> {
                    val action = message.action
                    when (action.kind) {
                        PlaybackKind.PLAY -> if (!audioPlayerEngine.playbackState.value.isPlaying) audioPlayerEngine.togglePlayPause()
                        PlaybackKind.PAUSE -> if (audioPlayerEngine.playbackState.value.isPlaying) audioPlayerEngine.togglePlayPause()
                        PlaybackKind.SEEK -> audioPlayerEngine.seekTo(action.positionMs)
                        PlaybackKind.CHANGE_TRACK -> action.track?.let { audioPlayerEngine.playTrack(it) }
                        else -> Unit
                    }
                }
                else -> Unit
            }
        } catch (_: Exception) {}
    }

    fun sendPlaybackAction(action: SyncPlayback) {
        scope.launch {
            outgoingChannel?.let { send ->
                val msg = json.encodeToString(
                    SyncMessage.serializer(),
                    SyncMessage.Action(action)
                )
                send(msg)
            }
        }
    }

    fun disconnect() {
        clientJob?.cancel()
        clientJob = null
        _status.value = SyncConnectionStatus.DISCONNECTED
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
