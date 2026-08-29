package com.music.vivi.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.sync.PlaybackDeviceTarget
import com.music.vivi.sync.RemoteConnectionState
import com.music.vivi.sync.RemoteSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemoteSyncViewModel @Inject constructor(
    val syncManager: RemoteSyncManager
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteSyncSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: RemoteSyncViewModel = hiltViewModel()
) {
    val syncManager = viewModel.syncManager
    val scrollState = rememberScrollState()

    val connectionState by syncManager.connectionState.collectAsState()
    val playbackTarget by syncManager.playbackTarget.collectAsState()
    val roomState by syncManager.remoteRoomState.collectAsState()
    val statusMessage by syncManager.statusMessage.collectAsState()

    val savedHost by syncManager.hostFlow.collectAsState(initial = "192.168.1.10")
    val savedPort by syncManager.portFlow.collectAsState(initial = 8080)
    val savedPin by syncManager.pinFlow.collectAsState(initial = "1234")

    var hostInput by remember(savedHost) { mutableStateOf(savedHost) }
    var portInput by remember(savedPort) { mutableStateOf(savedPort.toString()) }
    var pinInput by remember(savedPin) { mutableStateOf(savedPin) }

    val isConnected = connectionState == RemoteConnectionState.CONNECTED
    val isConnecting = connectionState == RemoteConnectionState.CONNECTING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nocturne Remote Sync", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Seamlessly control music playing on your desktop PC or select your active output device over local Wi-Fi or Tailscale.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Playback Output Device Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Playback Device",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Phone Target
                        val isLocal = playbackTarget == PlaybackDeviceTarget.LOCAL
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { syncManager.setPlaybackTarget(PlaybackDeviceTarget.LOCAL) },
                            color = if (isLocal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (isLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This Device",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isLocal) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isLocal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Desktop Target
                        val isRemote = playbackTarget == PlaybackDeviceTarget.REMOTE_DESKTOP
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isConnected) {
                                        syncManager.setPlaybackTarget(PlaybackDeviceTarget.REMOTE_DESKTOP)
                                    }
                                },
                            color = if (isRemote) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = if (isRemote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isConnected) 1f else 0.4f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Desktop PC",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isRemote) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isRemote) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isConnected) 1f else 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // Connection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        val badgeColor = when (connectionState) {
                            RemoteConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                            RemoteConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
                            RemoteConnectionState.ERROR -> MaterialTheme.colorScheme.error
                            RemoteConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
                        }

                        val badgeText = when (connectionState) {
                            RemoteConnectionState.CONNECTED -> "CONNECTED"
                            RemoteConnectionState.CONNECTING -> "CONNECTING..."
                            RemoteConnectionState.ERROR -> "ERROR"
                            RemoteConnectionState.DISCONNECTED -> "DISCONNECTED"
                        }

                        Badge(containerColor = badgeColor) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isConnected) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("Desktop IP / Tailscale Host") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { portInput = it },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = { Text("PIN") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val port = portInput.toIntOrNull() ?: 8080
                                syncManager.connect(hostInput.trim(), port, pinInput.trim())
                            },
                            enabled = !isConnecting && hostInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConnecting) "Connecting..." else "Pair & Connect")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { syncManager.disconnect() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Remote Now Playing & Controls
            AnimatedVisibility(visible = isConnected && roomState != null) {
                val track = roomState?.current_track
                val isPlaying = roomState?.is_playing ?: false
                val volume = roomState?.volume ?: 1.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Now Playing on Desktop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (track != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = track.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No track currently playing",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Playback Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { syncManager.sendPrevious() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                            }

                            FilledIconButton(
                                onClick = {
                                    if (isPlaying) syncManager.sendPause() else syncManager.sendPlay()
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = { syncManager.sendNext() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next")
                            }
                        }

                        // Volume Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = volume.toFloat(),
                                onValueChange = { syncManager.sendVolume(it) },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
