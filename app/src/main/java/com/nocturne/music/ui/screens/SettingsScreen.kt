package com.nocturne.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.sync.SyncConnectionStatus
import com.nocturne.music.ui.theme.*
import com.nocturne.music.ui.viewmodel.SyncViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    syncViewModel: SyncViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()

    val syncStatus by syncViewModel.status.collectAsState()
    val syncErrorMessage by syncViewModel.errorMessage.collectAsState()
    val connectedHost by syncViewModel.connectedHost.collectAsState()

    // Settings state
    var isPureBlack by remember { mutableStateOf(false) }
    var isDynamicColor by remember { mutableStateOf(false) }
    var selectedAccentIndex by remember { mutableIntStateOf(0) }
    val accents = listOf(
        "Nocturne" to NocturnePurple,
        "Cyan" to Color(0xFF00E5FF),
        "Blue" to Color(0xFF2979FF),
        "Emerald" to Color(0xFF00E676),
        "Rose" to Color(0xFFFF4081),
        "Monochrome" to Color(0xFFFFFFFF)
    )

    var audioQuality by remember { mutableStateOf("High (Opus 160kbps)") }
    var audioNormalization by remember { mutableStateOf(true) }
    var skipSilence by remember { mutableStateOf(false) }

    var syncedLyrics by remember { mutableStateOf(true) }
    var wordByWordLyrics by remember { mutableStateOf(true) }

    var hostInput by remember { mutableStateOf("192.168.1.10") }
    var pinInput by remember { mutableStateOf("1234") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPureBlack) Color.Black else NocturneDarkBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // 1. Appearance & Theme
        SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
            SettingsRow(
                title = "Pure Black (AMOLED)",
                subtitle = "Deep true black background for OLED displays",
                trailing = {
                    Switch(
                        checked = isPureBlack,
                        onCheckedChange = { isPureBlack = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accents[selectedAccentIndex].second
                        )
                    )
                }
            )

            SettingsRow(
                title = "Dynamic Material You",
                subtitle = "Harmonize colors with your Android wallpaper",
                trailing = {
                    Switch(
                        checked = isDynamicColor,
                        onCheckedChange = { isDynamicColor = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accents[selectedAccentIndex].second
                        )
                    )
                }
            )

            // Accent Color Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accents.forEachIndexed { index, (name, color) ->
                        val isSelected = selectedAccentIndex == index
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAccentIndex = index }
                        )
                    }
                }
            }
        }

        // 2. Playback & Audio
        SettingsSection(title = "Playback & Audio", icon = Icons.Default.MusicNote) {
            SettingsRow(
                title = "Audio Quality",
                subtitle = audioQuality,
                trailing = {
                    IconButton(onClick = {
                        audioQuality = when (audioQuality) {
                            "High (Opus 160kbps)" -> "Auto"
                            "Auto" -> "Low (AAC 48kbps)"
                            else -> "High (Opus 160kbps)"
                        }
                    }) {
                        Icon(Icons.Default.Tune, contentDescription = "Change Quality", tint = TextMuted)
                    }
                }
            )

            SettingsRow(
                title = "Loudness Normalization",
                subtitle = "Equalize volume across tracks using ReplayGain audio config",
                trailing = {
                    Switch(
                        checked = audioNormalization,
                        onCheckedChange = { audioNormalization = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accents[selectedAccentIndex].second
                        )
                    )
                }
            )

            SettingsRow(
                title = "Skip Silence",
                subtitle = "Automatically skip silent intros and outros",
                trailing = {
                    Switch(
                        checked = skipSilence,
                        onCheckedChange = { skipSilence = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accents[selectedAccentIndex].second
                        )
                    )
                }
            )
        }

        // 3. Nocturne Remote Sync (PC Control)
        SettingsSection(title = "Nocturne Remote Sync", icon = Icons.Default.Devices) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connection Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = when (syncStatus) {
                            SyncConnectionStatus.CONNECTED -> "CONNECTED"
                            SyncConnectionStatus.CONNECTING -> "CONNECTING..."
                            SyncConnectionStatus.AUTHENTICATING -> "AUTHENTICATING..."
                            SyncConnectionStatus.DISCONNECTED -> "DISCONNECTED"
                            SyncConnectionStatus.ERROR -> "ERROR"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (syncStatus == SyncConnectionStatus.CONNECTED) Color(0xFF00E676) else TextMuted
                    )
                }

                if (syncErrorMessage != null) {
                    Text(
                        text = syncErrorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5252)
                    )
                }

                if (syncStatus != SyncConnectionStatus.CONNECTED) {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("PC IP / Tailscale Host (e.g. 192.168.1.10:8080)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accents[selectedAccentIndex].second,
                            focusedLabelColor = accents[selectedAccentIndex].second,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Security PIN (e.g. 1234)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accents[selectedAccentIndex].second,
                            focusedLabelColor = accents[selectedAccentIndex].second,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            syncViewModel.connect(hostInput.trim(), pinInput.trim())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accents[selectedAccentIndex].second),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (syncStatus == SyncConnectionStatus.CONNECTING) "Connecting..." else "Pair & Connect")
                    }
                } else {
                    Text(
                        text = "Paired with ${connectedHost ?: "Desktop PC"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    OutlinedButton(
                        onClick = { syncViewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Disconnect", color = Color(0xFFFF5252))
                    }
                }
            }
        }

        // 4. Lyrics
        SettingsSection(title = "Lyrics", icon = Icons.Default.Mic) {
            SettingsRow(
                title = "Synchronized Lyrics",
                subtitle = "Auto-scroll lyrics synchronized with playback",
                trailing = {
                    Switch(
                        checked = syncedLyrics,
                        onCheckedChange = { syncedLyrics = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accents[selectedAccentIndex].second
                        )
                    )
                }
            )

            SettingsRow(
                title = "Word-by-Word Karaoke",
                subtitle = "Highlight individual syllables as they are sung",
                trailing = {
                    Switch(
                        checked = wordByWordLyrics,
                        onCheckedChange = { wordByWordLyrics = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accents[selectedAccentIndex].second
                        )
                    )
                }
            )
        }

        // 5. About Nocturne
        SettingsSection(title = "About", icon = Icons.Default.Info) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Nocturne Mobile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Version 0.1.0-alpha",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents[selectedAccentIndex].second
                )
                Text(
                    text = "Fast, native Android music client with direct YouTube streaming, synchronized lyrics, and seamless desktop synchronization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NocturneDarkCard)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        trailing()
    }
}
