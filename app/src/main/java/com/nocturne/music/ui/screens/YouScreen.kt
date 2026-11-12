package com.nocturne.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nocturne.music.R
import com.nocturne.music.data.model.Account
import com.nocturne.music.data.model.Song
import com.nocturne.music.data.model.UiState
import com.nocturne.music.data.scrobbling.LastFM
import com.nocturne.music.data.settings.AppSettings
import com.nocturne.music.download.Downloads
import com.nocturne.music.sync.NocturnePlaybackTarget
import com.nocturne.music.sync.NocturneSyncManager
import com.nocturne.music.sync.NocturneSyncState
import com.nocturne.music.ui.components.PAGE_GUTTER
import com.nocturne.music.ui.components.ReplayCardRowSkeleton
import com.nocturne.music.ui.haptics.Haptic
import com.nocturne.music.ui.haptics.rememberHaptics
import com.nocturne.music.ui.replay.ReplayCreditCard
import com.nocturne.music.ui.replay.ReplayHeroCard
import kotlinx.coroutines.launch

@Composable
fun YouScreen(
    account: Account?,
    signedIn: Boolean,
    historyState: UiState<List<Song>>,
    replayCards: List<ReplayHeroCard>,
    replayLoading: Boolean,
    memberSince: String?,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onPlayHistorySong: (List<Song>, Int) -> Unit,
    onPlayDownloadedSong: (List<Song>, Int) -> Unit,
    onRefreshHistory: () -> Unit,
    onOpenFullStats: () -> Unit,
    onSignIn: () -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Downloads list
    val savedDownloads by Downloads.saved.collectAsStateWithLifecycle()
    var downloadedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    LaunchedEffect(savedDownloads) {
        downloadedSongs = Downloads.getDownloadedSongs(context)
    }

    LaunchedEffect(Unit) {
        onRefreshHistory()
    }

    // Sync State
    val syncState by NocturneSyncManager.connectionState.collectAsStateWithLifecycle()
    val syncStatus by NocturneSyncManager.statusMessage.collectAsStateWithLifecycle()
    val discoveredDevice by NocturneSyncManager.discoveredDevice.collectAsStateWithLifecycle()
    val savedDevices by NocturneSyncManager.savedDevices.collectAsStateWithLifecycle()
    val activeConnectedDevice by NocturneSyncManager.activeConnectedDevice.collectAsStateWithLifecycle()

    var inputHost by remember { mutableStateOf("") }
    var inputPort by remember { mutableStateOf("") }
    var inputPin by remember { mutableStateOf("") }

    // Once connected, clear the IP, port and PIN fields ready for the next device
    LaunchedEffect(syncState) {
        if (syncState == NocturneSyncState.CONNECTED) {
            inputHost = ""
            inputPort = ""
            inputPin = ""
        }
    }

    // Last.fm live stats
    val lastfmUsername by AppSettings.lastfmUsername.collectAsStateWithLifecycle()
    var lastFmStats by remember { mutableStateOf<LastFM.LastFmStats?>(null) }
    var loadingLastFm by remember { mutableStateOf(false) }

    LaunchedEffect(lastfmUsername) {
        if (lastfmUsername.isNotBlank()) {
            loadingLastFm = true
            lastFmStats = LastFM.getUserStats(lastfmUsername)
            loadingLastFm = false
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        // ── 1. Profile Header ──────────────────────────────────────────
        item(key = "you-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val avatarUrl = account?.thumbnailUrl
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = account.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (signedIn) (account?.name ?: "Account") else "Guest Listener",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (signedIn) (account?.email?.ifBlank { "Signed In" } ?: "Signed In") else "Tap below to connect YouTube Music",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        if (!signedIn) {
            item(key = "you-signin-card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onSignIn() }
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign in to Nocturne",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Sync your personal playlists, likes, and listening stats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = onSignIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }
        }

        // ── 2. History Side-Scrolling Shelf ────────────────────────────
        item(key = "you-history-shelf") {
            val historySongs = (historyState as? UiState.Success)?.data.orEmpty()
            ShelfListSection(
                title = stringResource(R.string.history),
                songs = historySongs,
                onShowMore = onOpenHistory,
                onSongClick = { song, index -> onPlayHistorySong(historySongs, index) },
                emptyText = "No history recorded yet",
            )
        }

        // ── 3. Downloads Side-Scrolling Shelf ──────────────────────────
        item(key = "you-downloads-shelf") {
            ShelfListSection(
                title = stringResource(R.string.downloads),
                songs = downloadedSongs,
                onShowMore = onOpenDownloads,
                onSongClick = { song, index -> onPlayDownloadedSong(downloadedSongs, index) },
                emptyText = "No downloaded songs",
            )
        }

        // ── 4. Listening Stats (1 Main Card) ───────────────────────────
        item(key = "you-stats-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Listening Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        haptics.play(Haptic.Select)
                        onOpenFullStats()
                    },
                )
            }
        }

        item(key = "you-stats-card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (replayLoading) {
                    ReplayCardRowSkeleton()
                } else if (replayCards.isNotEmpty()) {
                    val mainCard = replayCards.first()
                    ReplayCreditCard(
                        label = mainCard.label,
                        value = mainCard.value,
                        detail = mainCard.detail,
                        artworkUrl = mainCard.artworkUrl,
                        holder = account?.name.orEmpty().ifBlank { "Listener" },
                        memberSince = memberSince,
                        onClick = {
                            haptics.play(Haptic.Select)
                            onOpenFullStats()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { onOpenFullStats() }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Start listening to see your stats",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Minutes, top artists, and songs will appear here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ── Last.fm Scrobble Stats Preview (if configured) ─────────────
        if (lastfmUsername.isNotBlank() && lastFmStats != null) {
            item(key = "you-lastfm-stats") {
                val stats = lastFmStats!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFBA0000).copy(alpha = 0.12f))
                        .clickable { onOpenFullStats() }
                        .padding(16.dp),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Last.fm • @${stats.username}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252),
                            )
                            Text(
                                text = "${stats.totalScrobbles} scrobbles",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (stats.topArtists.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Top artists: " + stats.topArtists.take(3).joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // ── 5. Nocturne Sync (PC OTP Setup) ────────────────────────────
        item(key = "you-sync-header") {
            Text(
                text = "Nocturne Sync (PC Link)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
            )
        }

        item(key = "you-sync-section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Computer,
                        contentDescription = null,
                        tint = when (syncState) {
                            NocturneSyncState.CONNECTED -> Color(0xFF4CAF50)
                            NocturneSyncState.FAILED_BLOCKED -> MaterialTheme.colorScheme.error
                            NocturneSyncState.CONNECTING -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Link Device to Desktop",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (syncState) {
                                NocturneSyncState.CONNECTED -> Color(0xFF4CAF50)
                                NocturneSyncState.FAILED_BLOCKED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                // ── Saved Devices ──────────────────────────────
                if (savedDevices.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Saved Devices",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    savedDevices.forEach { dev ->
                        val isThisConnected = syncState == NocturneSyncState.CONNECTED && activeConnectedDevice?.id == dev.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Computer,
                                contentDescription = null,
                                tint = if (isThisConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${dev.host}:${dev.port}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isThisConnected) {
                                TextButton(
                                    onClick = { NocturneSyncManager.disconnect() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("Disconnect")
                                }
                            } else {
                                Button(
                                    onClick = { NocturneSyncManager.connectToSaved(dev) },
                                    enabled = syncState != NocturneSyncState.CONNECTING,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text("Connect")
                                }
                            }
                            IconButton(
                                onClick = { NocturneSyncManager.removeSavedDevice(dev.id) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // ── Pair New PC Form (Inputs kept clean & empty ready for next device) ──
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (savedDevices.isEmpty()) "Pair Device with PC" else "Pair Another PC",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))

                if (discoveredDevice != null && syncState == NocturneSyncState.DISCONNECTED) {
                    TextButton(
                        onClick = {
                            inputHost = NocturneSyncManager.discoveredIp.value.orEmpty()
                            inputPort = NocturneSyncManager.discoveredPort.value.toString()
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "Detected nearby: $discoveredDevice (Tap to fill)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inputHost,
                        onValueChange = { inputHost = it },
                        label = { Text("PC IP Address") },
                        placeholder = { Text("192.168.1.xxx") },
                        singleLine = true,
                        modifier = Modifier.weight(1.8f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = inputPort,
                        onValueChange = { inputPort = it },
                        label = { Text("Port") },
                        placeholder = { Text("8080") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        ),
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputPin,
                    onValueChange = { inputPin = it },
                    label = { Text("Pairing PIN / OTP (from Desktop)") },
                    placeholder = { Text("6-digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    ),
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (syncState == NocturneSyncState.CONNECTED && activeConnectedDevice == null) {
                        Button(
                            onClick = { NocturneSyncManager.disconnect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                val portNum = inputPort.toIntOrNull() ?: 8080
                                NocturneSyncManager.startConnect(inputHost.trim(), portNum, inputPin.trim())
                            },
                            enabled = syncState != NocturneSyncState.CONNECTING &&
                                inputHost.isNotBlank() && inputPin.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            if (syncState == NocturneSyncState.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Text("Connect & Save")
                            }
                        }
                    }
                }
            }
        }

        item(key = "you-bottom-spacer") {
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ShelfListSection(
    title: String,
    songs: List<Song>,
    onShowMore: () -> Unit,
    onSongClick: (Song, Int) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Show more",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onShowMore() },
            )
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(songs.take(5), key = { _, s -> s.videoId }) { index, song ->
                    Column(
                        modifier = Modifier
                            .width(116.dp)
                            .clickable { onSongClick(song, index) },
                    ) {
                        val artwork = song.thumbnailUrl
                        if (artwork != null) {
                            AsyncImage(
                                model = artwork,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(116.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = song.artist.ifBlank { "Unknown Artist" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item(key = "$title-show-more-card") {
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onShowMore() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Show more",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Show more",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
