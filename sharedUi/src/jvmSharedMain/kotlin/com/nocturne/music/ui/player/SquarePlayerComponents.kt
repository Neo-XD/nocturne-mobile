package com.nocturne.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nocturne.music.data.NerdStats
import com.nocturne.music.data.model.Song
import com.nocturne.music.data.model.artworkAt
import com.nocturne.music.sharedui.resources.Res
import com.nocturne.music.sharedui.resources.ic_player_next
import com.nocturne.music.sharedui.resources.ic_player_pause
import com.nocturne.music.sharedui.resources.ic_player_play
import com.nocturne.music.sharedui.resources.ic_player_previous
import com.nocturne.music.sharedui.resources.pause
import com.nocturne.music.sharedui.resources.play
import com.nocturne.music.sharedui.resources.widget_next
import com.nocturne.music.sharedui.resources.widget_previous
import com.nocturne.music.ui.haptics.Haptic
import com.nocturne.music.ui.haptics.rememberHaptics
import com.nocturne.music.ui.icons.NocturneIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/**
 * Classic Nocturne v0.3 blurred album art background:
 * 800ms crossfade on artwork change with a live 80dp Haze frosted blur,
 * subtle dark tint (30%), and film grain noise (0.15f). Falls back to
 * [FullArtworkBlurBackdrop] when dynamic blur is disabled or unsupported.
 */
@Composable
internal fun V3BlurredAlbumBackdrop(
    imageUrl: String?,
    fallbackImage: ImageBitmap?,
    reduceDynamicBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    if (reduceDynamicBlur || !renderEffectBlurSupported) {
        FullArtworkBlurBackdrop(image = fallbackImage, modifier = modifier)
        return
    }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            targetState = imageUrl,
            transitionSpec = {
                fadeIn(tween(800)) togetherWith fadeOut(tween(800))
            },
            label = "v3BlurBackground",
        ) { url ->
            if (url != null) {
                val blurHazeState = remember(url) { HazeState() }
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = url.artworkAt(256),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = blurHazeState),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeEffect(
                                state = blurHazeState,
                                style = HazeStyle(
                                    backgroundColor = Color.Black,
                                    blurRadius = 80.dp,
                                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.30f))),
                                    noiseFactor = 0.15f,
                                    fallbackTint = HazeTint(Color.Black.copy(alpha = 0.55f)),
                                ),
                            ),
                    )
                }
            } else {
                FullArtworkBlurBackdrop(image = fallbackImage)
            }
        }
    }
}

/** Active middle stage in Square player mode. */
internal enum class SquareStage {
    COVER,
    LYRICS,
    QUEUE,
    INFO,
}

/**
 * Square-inspired top bar inside the expanded player:
 * - Left: circular glass collapse button
 * - Center: crossfading playback source or active panel label with soft shadow
 * - Right: circular glass Audio Output / Devices button
 */
@Composable
internal fun SquarePlayerTopBar(
    stage: SquareStage,
    sourceLabel: String,
    onOpenSource: () -> Unit,
    onCollapse: (() -> Unit)?,
    onOpenOutput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val partyState by PlayerPlatform.host.party.collectAsStateWithLifecycle()
    val inParty = partyState.inParty
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCollapse != null) {
            SquareGlassDisc(
                size = 40.dp,
                onClick = {
                    haptics.play(Haptic.Tap)
                    onCollapse()
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }

        Crossfade(
            targetState = when (stage) {
                SquareStage.LYRICS -> "Lyrics"
                SquareStage.QUEUE -> "Up Next"
                SquareStage.INFO -> "Track Info"
                SquareStage.COVER -> sourceLabel.ifBlank { "Now Playing" }
            },
            animationSpec = tween(220),
            label = "squareTopTitle",
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 1f),
                        blurRadius = 12f,
                    ),
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (stage == SquareStage.COVER) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onOpenSource() }
                        } else Modifier,
                    ),
            )
        }

        SquareGlassDisc(
            size = 40.dp,
            onClick = {
                haptics.play(Haptic.Tap)
                onOpenOutput()
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = "Audio Output",
                tint = if (inParty) Color(0xFF1ED760) else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Square-inspired 5-control transport row:
 * [Shuffle] — [62dp Glass SkipBack] — [76dp Glass Play/Pause] — [62dp Glass SkipForward] — [Repeat]
 */
@Composable
internal fun SquareTransportRow(
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SquareToggleGlyph(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "Shuffle",
            active = shuffleEnabled,
            onClick = {
                haptics.play(if (shuffleEnabled) Haptic.ToggleOff else Haptic.ToggleOn)
                onToggleShuffle()
            },
        )

        SquareGlassDisc(
            size = 60.dp,
            enabled = previousEnabled,
            onClick = {
                haptics.play(Haptic.SkipPrevious)
                onPrevious()
            },
        ) {
            SquareDrawableIcon(
                icon = Res.drawable.ic_player_previous,
                contentDescription = stringResource(Res.string.widget_previous),
                size = 28.dp,
                alpha = if (previousEnabled) 1f else 0.35f,
            )
        }

        SquareGlassDisc(
            size = 76.dp,
            highlighted = true,
            onClick = {
                haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                onPlayPause()
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.6f),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(54.dp),
                    )
                }
                Crossfade(
                    targetState = isPlaying,
                    animationSpec = tween(180),
                    label = "squarePlayPause",
                ) { playing ->
                    SquareDrawableIcon(
                        icon = if (playing) Res.drawable.ic_player_pause else Res.drawable.ic_player_play,
                        contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
                        size = 34.dp,
                        alpha = 1f,
                    )
                }
            }
        }

        SquareGlassDisc(
            size = 60.dp,
            enabled = nextEnabled,
            onClick = {
                haptics.play(Haptic.SkipNext)
                onNext()
            },
        ) {
            SquareDrawableIcon(
                icon = Res.drawable.ic_player_next,
                contentDescription = stringResource(Res.string.widget_next),
                size = 28.dp,
                alpha = if (nextEnabled) 1f else 0.35f,
            )
        }

        SquareToggleGlyph(
            icon = if (repeatMode == RepeatModes.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = "Repeat",
            active = repeatMode != RepeatModes.OFF,
            onClick = {
                haptics.play(Haptic.Select)
                onCycleRepeat()
            },
        )
    }
}

/**
 * Square-inspired bottom segmented frosted glass capsule (44dp tall, 88% width)
 * switching between Cover, Lyrics, Queue, Audio Output/Pipeline, and Track Info.
 */
@Composable
internal fun SquareBottomPanelPill(
    stage: SquareStage,
    onSelectStage: (SquareStage) -> Unit,
    onOpenOutput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth(0.88f)
            .height(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.13f))
            .border(0.75.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquarePillTab(
            icon = Icons.Rounded.Album,
            label = "Cover",
            selected = stage == SquareStage.COVER,
            onClick = {
                haptics.play(Haptic.Select)
                onSelectStage(SquareStage.COVER)
            },
            modifier = Modifier.weight(1f),
        )
        SquarePillTab(
            icon = NocturneIcons.LyricsQuote,
            label = "Lyrics",
            selected = stage == SquareStage.LYRICS,
            onClick = {
                haptics.play(Haptic.Select)
                onSelectStage(if (stage == SquareStage.LYRICS) SquareStage.COVER else SquareStage.LYRICS)
            },
            modifier = Modifier.weight(1f),
        )
        SquarePillTab(
            icon = NocturneIcons.Queue,
            label = "Queue",
            selected = stage == SquareStage.QUEUE,
            onClick = {
                haptics.play(Haptic.Select)
                onSelectStage(if (stage == SquareStage.QUEUE) SquareStage.COVER else SquareStage.QUEUE)
            },
            modifier = Modifier.weight(1f),
        )
        SquarePillTab(
            icon = Icons.Rounded.Tune,
            label = "Audio",
            selected = false,
            onClick = {
                haptics.play(Haptic.Select)
                onOpenOutput()
            },
            modifier = Modifier.weight(1f),
        )
        SquarePillTab(
            icon = Icons.Rounded.Info,
            label = "Info",
            selected = stage == SquareStage.INFO,
            onClick = {
                haptics.play(Haptic.Select)
                onSelectStage(if (stage == SquareStage.INFO) SquareStage.COVER else SquareStage.INFO)
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SquarePillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.62f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SquareGlassDisc(
    size: Dp,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "squareDiscScale",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                Color.White.copy(alpha = if (highlighted) 0.22f else 0.14f),
            )
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = if (highlighted) 0.34f else 0.20f),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SquareToggleGlyph(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) Color.White.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SquareDrawableIcon(
    icon: DrawableResource,
    contentDescription: String,
    size: Dp,
    alpha: Float,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = Color.White.copy(alpha = alpha),
        modifier = Modifier.size(size),
    )
}

/**
 * Square-inspired inline Track Info & Credits panel shown when the Info tab is selected.
 */
@Composable
internal fun SquareTrackInfoPanel(
    song: Song,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String?, String) -> Unit,
    onOpenAudioPipeline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Track Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                SquareInfoRow("Title", song.title)
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                SquareInfoRow(
                    label = "Artist",
                    value = song.artist,
                    onClick = { onOpenArtist(song.artistId, song.artist) },
                )
                song.albumName?.takeIf { it.isNotBlank() }?.let { album ->
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    SquareInfoRow(
                        label = "Album",
                        value = album,
                        onClick = song.albumId?.let { id -> { onOpenAlbum(id) } },
                    )
                }
                song.playbackSource?.takeIf { it.isNotBlank() }?.let { src ->
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    SquareInfoRow("Playing From", src)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                .clickable { onOpenAudioPipeline() }
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Audio Stream",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                val stats = nerdStats
                if (stats != null) {
                    stats.mimeType?.let { SquareInfoRow("Format", it.substringAfter('/').uppercase(Locale.ROOT)) }
                    stats.bitrateKbps?.let { SquareInfoRow("Bitrate", "$it kbps") }
                    stats.sampleRateHz?.let { SquareInfoRow("Sample Rate", "%.1f kHz".format(Locale.ROOT, it / 1000f)) }
                    stats.bitDepth?.let { SquareInfoRow("Bit Depth", "$it-bit") }
                } else {
                    Text(
                        text = "Tap to inspect audio pipeline & output",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SquareInfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
