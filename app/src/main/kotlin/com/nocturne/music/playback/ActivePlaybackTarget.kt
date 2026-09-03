package com.nocturne.music.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.nocturne.music.sync.LocalRemoteSyncManager
import com.nocturne.music.sync.PlaybackDeviceTarget
import com.nocturne.music.sync.RemoteConnectionState

/**
 * The playing state of whichever device the controls act on, resolved by the same rule
 * PlayerConnection dispatches by, so an icon cannot report one device while a tap reaches another.
 */
@Composable
fun rememberIsPlayingOnActiveTarget(playerConnection: PlayerConnection): Boolean {
    val localIsPlaying by playerConnection.isPlaying.collectAsState()

    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val sync = LocalRemoteSyncManager.current
    val target by sync.playbackTarget.collectAsState()
    val connectionState by sync.connectionState.collectAsState()
    val roomState by sync.remoteRoomState.collectAsState()

    return when {
        // Matches PlayerConnection's dispatch predicate exactly; a connected desktop with no room
        // state yet reports not-playing rather than falling back to the local player it is not using.
        target == PlaybackDeviceTarget.REMOTE_DESKTOP &&
            connectionState == RemoteConnectionState.CONNECTED -> roomState?.is_playing == true

        isCasting -> castIsPlaying
        else -> localIsPlaying
    }
}
