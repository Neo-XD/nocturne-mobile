package com.nocturne.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.nocturne.music.core.model.RepeatMode
import com.nocturne.music.ui.components.SyncedLyricsView
import com.nocturne.music.ui.theme.*
import com.nocturne.music.ui.viewmodel.PlayerViewModel

@UnstableApi
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val track = playbackState.currentTrack ?: return

    var showLyrics by remember { mutableStateOf(false) }

    val currentMs = playbackState.positionMs
    val totalMs = playbackState.durationMs.coerceAtLeast(1L)
    val sliderValue = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NocturneDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = TextPrimary)
            }

            Text(
                text = "Playing from Nocturne",
                style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary, fontSize = 13.sp)
            )

            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Lyrics",
                    tint = if (showLyrics) NocturnePurple else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showLyrics) {
            Box(modifier = Modifier.weight(1f)) {
                SyncedLyricsView(
                    lyrics = lyrics,
                    currentPositionMs = currentMs,
                    onLineClick = { viewModel.seekTo(it) }
                )
            }
        } else {
            // Big Album Artwork
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Title & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artists,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { /* Toggle Like */ }) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider
            Slider(
                value = sliderValue,
                onValueChange = { frac ->
                    viewModel.seekTo((frac * totalMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = NocturnePurple,
                    activeTrackColor = NocturnePurple,
                    inactiveTrackColor = NocturneDarkElevated
                )
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatDuration(currentMs), style = MaterialTheme.typography.labelSmall)
                Text(text = formatDuration(totalMs), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffle) NocturnePurple else TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.previous() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = NocturnePurple,
                    modifier = Modifier.size(64.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = NocturneDarkBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    Icon(
                        imageVector = when (playbackState.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) NocturnePurple else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
