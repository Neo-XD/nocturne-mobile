package com.music.bitchord.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
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
import com.music.bitchord.R
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.sync.NocturnePlaybackTarget
import com.music.bitchord.sync.NocturneSyncManager
import com.music.bitchord.sync.NocturneSyncState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.ReplayCardRowSkeleton
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.replay.ReplayCreditCard
import com.music.bitchord.ui.replay.ReplayHeroCard
import kotlinx.coroutines.launch

@Composable
fun YouScreen(
    account: Account?,
    signedIn: Boolean,
    replayCards: List<ReplayHeroCard>,
    replayLoading: Boolean,
    memberSince: String?,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFullStats: () -> Unit,
    onSignIn: () -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Sync State
    val syncState by NocturneSyncManager.connectionState.collectAsStateWithLifecycle()
    val syncStatus by NocturneSyncManager.statusMessage.collectAsStateWithLifecycle()
    val syncHost by NocturneSyncManager.host.collectAsStateWithLifecycle()
    val syncPort by NocturneSyncManager.port.collectAsStateWithLifecycle()
    val syncPin by NocturneSyncManager.pin.collectAsStateWithLifecycle()

    var inputHost by remember(syncHost) { mutableStateOf(syncHost) }
    var inputPort by remember(syncPort) { mutableStateOf(syncPort.toString()) }
    var inputPin by remember(syncPin) { mutableStateOf(syncPin) }

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

        // ── 2. Listening Stats (1 Main Card) ───────────────────────────
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

        // ── 3. Quick Actions (Downloads, History) ──────────────────────
        item(key = "you-shortcuts-header") {
            Text(
                text = "Library & History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
            )
        }

        item(key = "you-shortcuts") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                YouActionRow(
                    icon = Icons.Rounded.Download,
                    title = stringResource(R.string.downloads),
                    subtitle = stringResource(R.string.downloaded_songs),
                    onClick = onOpenDownloads,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                )
                YouActionRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.history),
                    subtitle = "Recently played tracks and stations",
                    onClick = onOpenHistory,
                )
            }
        }

        // ── 4. Nocturne Sync (PC OTP Setup) ────────────────────────────
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

                Spacer(Modifier.height(14.dp))

                if (syncState != NocturneSyncState.CONNECTED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = inputHost,
                            onValueChange = { inputHost = it },
                            label = { Text("PC IP Address") },
                            placeholder = { Text("192.168.1.10") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        OutlinedTextField(
                            value = inputPort,
                            onValueChange = { inputPort = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Port") },
                            placeholder = { Text("8080") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { inputPin = it.trim() },
                        label = { Text("Pairing PIN / OTP") },
                        placeholder = { Text("e.g. 123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                val parsedPort = inputPort.toIntOrNull() ?: 8080
                                NocturneSyncManager.saveConfig(inputHost, parsedPort, inputPin)
                                NocturneSyncManager.startConnect(inputHost, parsedPort, inputPin)
                            },
                            enabled = syncState != NocturneSyncState.CONNECTING && inputHost.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
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
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Connect to PC")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { NocturneSyncManager.disconnect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun YouActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}
