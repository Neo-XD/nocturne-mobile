package com.nocturne.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocturne.music.data.NerdStats
import com.nocturne.music.data.model.Song
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Material 3 Expressive (M3E) player backdrop:
 * Extracts expressive tonal surfaces from the artwork palette and paints
 * layered tonal container gradients with soft radial accent blooms for high
 * contrast and a clean Material You expressive aesthetic.
 */
@Composable
internal fun M3EPlayerBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkColors(imageUrl)
    val seedPrimary = palette.colors.getOrNull(0) ?: Color(0xFF4F378B)
    val seedSecondary = palette.colors.getOrNull(1) ?: Color(0xFF332D41)
    val seedTertiary = palette.colors.getOrNull(2) ?: Color(0xFF492532)

    val tonalSurface by animateColorAsState(
        targetValue = lerp(Color(0xFF121018), seedPrimary, 0.28f),
        animationSpec = tween(550),
        label = "m3eSurface",
    )
    val tonalAccent by animateColorAsState(
        targetValue = lerp(Color(0xFF1F1A2B), seedSecondary, 0.45f),
        animationSpec = tween(550),
        label = "m3eAccent",
    )
    val tonalHighlight by animateColorAsState(
        targetValue = lerp(Color(0xFF2B2238), seedTertiary, 0.40f),
        animationSpec = tween(550),
        label = "m3eHighlight",
    )

    Box(modifier = modifier.fillMaxSize().background(tonalSurface)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to tonalAccent.copy(alpha = 0.85f),
                    0.55f to tonalSurface,
                    1.0f to Color(0xFF0E0C12),
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tonalHighlight.copy(alpha = 0.48f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.18f),
                    radius = size.maxDimension * 0.58f,
                ),
                center = Offset(size.width * 0.82f, size.height * 0.18f),
                radius = size.maxDimension * 0.58f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tonalAccent.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(size.width * 0.16f, size.height * 0.78f),
                    radius = size.maxDimension * 0.52f,
                ),
                center = Offset(size.width * 0.16f, size.height * 0.78f),
                radius = size.maxDimension * 0.52f,
            )
        }
    }
}

/** Active middle stage in Nocturne player mode. */
internal enum class SquareStage {
    COVER,
    LYRICS,
    QUEUE,
    EFFECTS,
    INFO,
}

/**
 * Top bar inside the expanded Nocturne player:
 * - Left: optional circular glass collapse button
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
                SquareStage.EFFECTS -> "Playback Effects"
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
 * 5-control transport row:
 * [Shuffle] — [60dp SkipBack] — [76dp Play/Pause] — [60dp SkipForward] — [Repeat]
 * Honors [transparentButtons] to hide the circular disc background if the user prefers.
 */
@Composable
internal fun SquareTransportRow(
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    transparentButtons: Boolean = false,
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
            transparent = transparentButtons,
            onClick = {
                haptics.play(if (shuffleEnabled) Haptic.ToggleOff else Haptic.ToggleOn)
                onToggleShuffle()
            },
        )

        SquareGlassDisc(
            size = 60.dp,
            enabled = previousEnabled,
            transparent = transparentButtons,
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
            transparent = transparentButtons,
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
            transparent = transparentButtons,
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
            transparent = transparentButtons,
            onClick = {
                haptics.play(Haptic.Select)
                onCycleRepeat()
            },
        )
    }
}

/**
 * Bottom segmented frosted capsule (44dp tall, 88% width)
 * switching between Cover, Lyrics, Queue, Playback Effects (Sliders), and Track Info.
 */
@Composable
internal fun SquareBottomPanelPill(
    stage: SquareStage,
    effectsActive: Boolean,
    onSelectStage: (SquareStage) -> Unit,
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
            label = "Effects",
            selected = stage == SquareStage.EFFECTS,
            marked = effectsActive,
            onClick = {
                haptics.play(Haptic.Select)
                onSelectStage(if (stage == SquareStage.EFFECTS) SquareStage.COVER else SquareStage.EFFECTS)
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
    marked: Boolean = false,
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
        if (marked && !selected) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                selected -> Color.White
                marked -> Color.White.copy(alpha = 0.9f)
                else -> Color.White.copy(alpha = 0.62f)
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SquareGlassDisc(
    size: Dp,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    transparent: Boolean = false,
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
            .then(
                if (transparent) {
                    Modifier
                } else {
                    Modifier
                        .background(
                            Color.White.copy(alpha = if (highlighted) 0.22f else 0.14f),
                        )
                        .border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = if (highlighted) 0.34f else 0.20f),
                            shape = CircleShape,
                        )
                },
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
    transparent: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active && !transparent) Color.White.copy(alpha = 0.18f) else Color.Transparent)
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

private data class PlaybackPreset(
    val label: String,
    val speed: Float,
    val pitch: Float,
)

private val PlaybackEffectPresets = listOf(
    PlaybackPreset("Normal", 1.0f, 1.0f),
    PlaybackPreset("Slowed", 0.85f, 0.90f),
    PlaybackPreset("Deep Slowed", 0.78f, 0.82f),
    PlaybackPreset("Nightcore", 1.25f, 1.22f),
    PlaybackPreset("Sped Up", 1.20f, 1.0f),
)

/**
 * Square-style inline Playback Controls / Audio Effects panel (opened from the Sliders tab):
 * live Speed slider, Pitch slider, quick effect presets, and one-tap Reset.
 */
@Composable
internal fun SquareEffectsPanel(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val modified = speed != 1.0f || pitch != 1.0f
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Presets row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaybackEffectPresets.forEach { preset ->
                val active = kotlin.math.abs(speed - preset.speed) < 0.02f &&
                    kotlin.math.abs(pitch - preset.pitch) < 0.02f
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (active) Color.White else Color.White.copy(alpha = 0.14f),
                        )
                        .border(
                            0.75.dp,
                            Color.White.copy(alpha = if (active) 0.8f else 0.22f),
                            CircleShape,
                        )
                        .clickable {
                            haptics.play(Haptic.Select)
                            onSpeedChange(preset.speed)
                            onPitchChange(preset.pitch)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.Black else Color.White,
                    )
                }
            }
        }

        // Speed & Pitch controls card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Playback Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    if (modified) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable {
                                    haptics.play(Haptic.Tap)
                                    onSpeedChange(1.0f)
                                    onPitchChange(1.0f)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                }

                // Speed slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Speed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                        Text(
                            text = "%.2fx".format(Locale.ROOT, speed),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Slider(
                        value = speed,
                        onValueChange = { raw ->
                            val snapped = (raw * 20f).roundToInt() / 20f
                            onSpeedChange(snapped.coerceIn(0.5f, 2.0f))
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        ),
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Pitch slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Pitch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                        Text(
                            text = "%.2fx".format(Locale.ROOT, pitch),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Slider(
                        value = pitch,
                        onValueChange = { raw ->
                            val snapped = (raw * 20f).roundToInt() / 20f
                            onPitchChange(snapped.coerceIn(0.5f, 2.0f))
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Inline Track Info & Credits panel shown when the Info tab is selected.
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
