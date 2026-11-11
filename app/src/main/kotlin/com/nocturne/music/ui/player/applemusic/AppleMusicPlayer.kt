/**
 * Nocturne Music Project (C) 2026
 * Apple Music Now Playing Player
 */

package com.nocturne.music.ui.player.applemusic

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nocturne.music.LocalDatabase
import com.nocturne.music.LocalPlayerConnection
import com.nocturne.music.nocturne.AudioDeviceBottomSheet
import com.nocturne.music.sync.LocalRemoteSyncManager
import com.nocturne.music.sync.RemoteConnectionState
import com.nocturne.music.ui.component.BottomSheetState
import com.nocturne.music.ui.component.ContinuousCornerShape
import com.nocturne.music.ui.component.GLASS_CORNER_SMOOTHING
import com.nocturne.music.ui.component.LocalMenuState
import com.nocturne.music.ui.component.rememberBottomSheetState
import com.nocturne.music.ui.menu.PlayerMenu
import com.nocturne.music.ui.player.InlineLyricsView
import com.nocturne.music.ui.player.Queue
import com.nocturne.music.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playerConnection, isPlaying) {
        while (isActive) {
            position = playerConnection.player.currentPosition.coerceAtLeast(0L)
            duration = playerConnection.player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    val artworkPalette = rememberArtworkColors(mediaMetadata?.thumbnailUrl)

    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showAudioOutputSheet by remember { mutableStateOf(false) }

    val syncManager = LocalRemoteSyncManager.current
    val syncConnectionState by syncManager.connectionState.collectAsState()
    val isSyncConnected = syncConnectionState == RemoteConnectionState.CONNECTED

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var currentVolumeFraction by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.coerceAtLeast(1f))
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "artScale",
    )

    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        collapsedBound = 0.dp,
        expandedBound = 1000.dp,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Liquid Animated Mesh Background
        MeshGradientBackground(
            palette = artworkPalette,
            trackKey = mediaMetadata?.id,
            continuous = isPlaying,
        )

        // Main Player UI Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { state.collapseSoft() }) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = mediaMetadata?.album?.title ?: mediaMetadata?.title ?: "Nocturne",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = {
                    mediaMetadata?.let { meta ->
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = meta,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {},
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. Central Squircle Album Artwork
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val artSize = minOf(maxWidth, maxHeight, 330.dp)
                val shape = remember { ContinuousCornerShape(28.dp, GLASS_CORNER_SMOOTHING) }

                Box(
                    modifier = Modifier
                        .size(artSize)
                        .scale(artScale)
                        .shadow(
                            elevation = 20.dp,
                            shape = shape,
                            spotColor = Color.Black.copy(alpha = 0.65f),
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                        )
                        .border(0.5.dp, Color(0x33FFFFFF), shape)
                        .clip(shape),
                ) {
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3. Title & Artist Row with Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val isFavorite = currentSong?.song?.liked == true
                IconButton(onClick = {
                    mediaMetadata?.let { meta ->
                        database.query {
                            currentSong?.song?.let { song ->
                                update(song.copy(liked = !song.liked))
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFA2D48) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4. Apple Music Thin Scrubber Slider
            ThinSlider(
                value = progress,
                onValueChange = { frac ->
                    val target = (frac * duration).toLong()
                    playerConnection.player.seekTo(target)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Timestamps: Elapsed & Remaining
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = makeTimeString(position),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.6f),
                )
                Text(
                    text = "-" + makeTimeString((duration - position).coerceAtLeast(0L)),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 5. Playback Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = { playerConnection.player.seekToPrevious() },
                    enabled = canSkipPrevious,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = if (canSkipPrevious) 0.9f else 0.3f),
                        modifier = Modifier.size(38.dp),
                    )
                }

                // Large Circular Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp),
                    )
                }

                IconButton(
                    onClick = { playerConnection.player.seekToNext() },
                    enabled = canSkipNext,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = if (canSkipNext) 0.9f else 0.3f),
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 6. Volume Slider Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                ThinSlider(
                    value = currentVolumeFraction,
                    onValueChange = { frac ->
                        currentVolumeFraction = frac
                        val targetVol = (frac * maxVolume).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    },
                    modifier = Modifier.weight(1f),
                    activeTrackColor = Color.White.copy(alpha = 0.85f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 7. Bottom Action Row: Lyrics | Nocturne Sync / Audio Output | Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    showLyrics = !showLyrics
                    if (showLyrics) showQueue = false
                }) {
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) Color.White else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(26.dp),
                    )
                }

                // Middle Button: Nocturne Remote Sync & Audio Device Output
                IconButton(onClick = { showAudioOutputSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSyncConnected) Icons.Rounded.Computer else Icons.Rounded.Speaker,
                            contentDescription = "Nocturne Sync & Output",
                            tint = if (isSyncConnected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(24.dp),
                        )
                        if (isSyncConnected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.TopEnd)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    showQueue = !showQueue
                    if (showQueue) showLyrics = false
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueue) Color.White else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        // Lyrics View Overlay
        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                InlineLyricsView(
                    mediaMetadata = mediaMetadata,
                    showLyrics = showLyrics,
                    positionProvider = { position },
                )
            }
        }

        // Queue View Overlay
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.90f))
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Queue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    navController = navController,
                    background = Color.Transparent,
                    onBackgroundColor = Color.White,
                    TextBackgroundColor = Color.White.copy(alpha = 0.1f),
                    textButtonColor = Color.White,
                    iconButtonColor = Color.White,
                    pureBlack = false,
                    showInlineLyrics = showLyrics,
                    playerBackground = com.nocturne.music.constants.PlayerBackgroundStyle.APPLE_MUSIC,
                    onToggleLyrics = { showLyrics = !showLyrics },
                )
            }
        }

        if (showAudioOutputSheet) {
            AudioDeviceBottomSheet(
                onDismiss = { showAudioOutputSheet = false },
            )
        }
    }
}
