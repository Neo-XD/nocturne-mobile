/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nocturne.music.LocalPlayerAwareWindowInsets
import com.nocturne.music.LocalPlayerConnection
import com.nocturne.music.R
import com.nocturne.music.constants.PlayerBackgroundBlurRadiusKey
import com.nocturne.music.constants.PlayerBackgroundDimKey
import com.nocturne.music.constants.PlayerBackgroundMotionSpeedKey
import com.nocturne.music.constants.PlayerBackgroundSaturationKey
import com.nocturne.music.constants.PlayerBackgroundStyle
import com.nocturne.music.constants.PlayerBackgroundStyleKey
import com.nocturne.music.constants.PlayerGlassBorderKey
import com.nocturne.music.constants.SliderStyle
import com.nocturne.music.constants.SliderStyleKey
import com.nocturne.music.constants.SquigglySliderKey
import com.nocturne.music.constants.ShowAudioQualityBadgeKey
import com.nocturne.music.constants.FloatingNavBarKey
import com.nocturne.music.constants.SlimNavBarKey
import com.nocturne.music.ui.component.GlassyWarpBackground
import com.nocturne.music.ui.component.NocturneGlassCard
import com.nocturne.music.ui.component.rememberNocturneGlassBorderBrush
import com.nocturne.music.utils.rememberEnumPreference
import com.nocturne.music.utils.rememberPreference
import kotlin.math.roundToInt

/**
 * Convx-style Granular Player Theme Customizer with Live Interactive Preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerThemeCustomizerScreen(
    navController: NavController
) {
    val playerConnection = LocalPlayerConnection.current
    val currentMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    // Persistent preferences
    val (backgroundStyle, onBackgroundStyleChange) = rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GLASSY_WARP
    )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(
        PlayerBackgroundBlurRadiusKey,
        defaultValue = 75f
    )
    val (dimAmount, onDimAmountChange) = rememberPreference(
        PlayerBackgroundDimKey,
        defaultValue = 0.35f
    )
    val (saturation, onSaturationChange) = rememberPreference(
        PlayerBackgroundSaturationKey,
        defaultValue = 1.6f
    )
    val (motionSpeed, onMotionSpeedChange) = rememberPreference(
        PlayerBackgroundMotionSpeedKey,
        defaultValue = 1.0f
    )
    val (glassBorder, onGlassBorderChange) = rememberPreference(
        PlayerGlassBorderKey,
        defaultValue = true
    )
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.SLIM
    )
    val (squigglySlider, onSquigglySliderChange) = rememberPreference(
        SquigglySliderKey,
        defaultValue = false
    )
    val (showAudioQualityBadge, onShowAudioQualityBadgeChange) = rememberPreference(
        ShowAudioQualityBadgeKey,
        defaultValue = false
    )
    val (floatingNavBar, onFloatingNavBarChange) = rememberPreference(
        FloatingNavBarKey,
        defaultValue = true
    )
    val (slimNav, onSlimNavChange) = rememberPreference(
        SlimNavBarKey,
        defaultValue = false
    )

    // Sample artwork fallback if no track is playing
    val sampleArtwork = currentMetadata?.thumbnailUrl
        ?: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
    val sampleTitle = currentMetadata?.title?.takeIf { it.isNotBlank() } ?: "Nocturne Serenade"
    val sampleArtist = currentMetadata?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "Ambient Echoes"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        // Title row with reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Player Appearance",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = {
                    onBackgroundStyleChange(PlayerBackgroundStyle.GLASSY_WARP)
                    onBlurRadiusChange(75f)
                    onDimAmountChange(0.35f)
                    onSaturationChange(1.6f)
                    onMotionSpeedChange(1.0f)
                    onGlassBorderChange(true)
                    onSliderStyleChange(SliderStyle.SLIM)
                    onSquigglySliderChange(false)
                    onShowAudioQualityBadgeChange(false)
                    onFloatingNavBarChange(true)
                    onSlimNavChange(false)
                }
            ) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Fine-tune visual blur, lighting, organic warping motion, and frosted glass borders with real-time preview.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // -------------------------------------------------------------
        // LIVE INTERACTIVE PREVIEW CARD
        // -------------------------------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background rendering inside preview
                when (backgroundStyle) {
                    PlayerBackgroundStyle.GLASSY_WARP -> {
                        GlassyWarpBackground(
                            thumbnailUrl = sampleArtwork,
                            blurRadius = blurRadius.dp,
                            dimAlpha = dimAmount,
                            saturation = saturation,
                            motionSpeed = motionSpeed
                        )
                    }
                    PlayerBackgroundStyle.BLUR -> {
                        GlassyWarpBackground(
                            thumbnailUrl = sampleArtwork,
                            blurRadius = blurRadius.dp,
                            dimAlpha = dimAmount,
                            saturation = 1.0f,
                            motionSpeed = 0f
                        )
                    }
                    PlayerBackgroundStyle.LIVE_MESH -> {
                        GlassyWarpBackground(
                            thumbnailUrl = sampleArtwork,
                            blurRadius = blurRadius.dp,
                            dimAlpha = dimAmount,
                            saturation = saturation * 1.2f,
                            motionSpeed = motionSpeed * 1.5f
                        )
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                            Color.Black
                                        )
                                    )
                                )
                        )
                    }
                    PlayerBackgroundStyle.GLOW_ANIMATED -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            Color.Black
                                        )
                                    )
                                )
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        )
                    }
                }

                // Simulated Player UI Content inside card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top row: simulated tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = backgroundStyle.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Centered album art thumbnail
                    AsyncImage(
                        model = sampleArtwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (glassBorder) {
                                    Modifier.border(
                                        1.dp,
                                        rememberNocturneGlassBorderBrush(0.5f, 0.1f),
                                        RoundedCornerShape(20.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )

                    // Song Info + Controls in Frosted Glass Container
                    NocturneGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        showBorder = glassBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = sampleTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = sampleArtist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (showAudioQualityBadge) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "FLAC 24-bit",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val progressHeight = when (sliderStyle) {
                                SliderStyle.SLIM -> 3.dp
                                SliderStyle.WAVY -> 6.dp
                                else -> 5.dp
                            }
                            LinearProgressIndicator(
                                progress = { 0.42f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(progressHeight)
                                    .clip(RoundedCornerShape(progressHeight / 2)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Simulated playback controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // -------------------------------------------------------------
        // PRESET STYLE SELECTOR
        // -------------------------------------------------------------
        Text(
            text = "Background Preset",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val presets = listOf(
            PlayerBackgroundStyle.GLASSY_WARP to "Glassy Warp",
            PlayerBackgroundStyle.BLUR to "Dynamic Blur",
            PlayerBackgroundStyle.LIVE_MESH to "Live Mesh",
            PlayerBackgroundStyle.GLOW_ANIMATED to "Glow Animated",
            PlayerBackgroundStyle.GRADIENT to "Gradient",
            PlayerBackgroundStyle.DEFAULT to "Follow Theme"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(presets) { (style, label) ->
                val isSelected = backgroundStyle == style
                FilterChip(
                    selected = isSelected,
                    onClick = { onBackgroundStyleChange(style) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // -------------------------------------------------------------
        // GRANULAR CONTROLS
        // -------------------------------------------------------------
        Text(
            text = "Granular Parameters",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Blur Intensity Slider
        ControlSliderCard(
            title = "Blur Radius",
            valueText = "${blurRadius.roundToInt()} dp",
            value = blurRadius,
            range = 20f..120f,
            onValueChange = onBlurRadiusChange,
            description = "Amount of optical blur applied to the album background"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Dim / Opacity Slider
        ControlSliderCard(
            title = "Overlay Dim",
            valueText = "${(dimAmount * 100).roundToInt()}%",
            value = dimAmount,
            range = 0.10f..0.75f,
            onValueChange = onDimAmountChange,
            description = "Darkness level to ensure text and control legibility"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Saturation Multiplier Slider
        ControlSliderCard(
            title = "Color Saturation",
            valueText = String.format("%.1fx", saturation),
            value = saturation,
            range = 0.8f..2.5f,
            onValueChange = onSaturationChange,
            description = "Boosts album artwork colors for vibrant ambient light"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Motion / Warping Speed
        Text(
            text = "Warping Motion Speed",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Text(
            text = "Controls breathing and liquid rotation speed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        val speedOptions = listOf(
            0f to "Off",
            0.5f to "Slow",
            1.0f to "Normal",
            1.5f to "Fast"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speedOptions.forEach { (speed, label) ->
                val isSelected = motionSpeed == speed
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = isSelected,
                    onClick = { onMotionSpeedChange(speed) },
                    label = {
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Frosted Glass Specular Border Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGlassBorderChange(!glassBorder) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Frosted Glass Border Highlights",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Adds acrylic specular reflections along card edges",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = glassBorder,
                    onCheckedChange = onGlassBorderChange,
                    thumbContent = {
                        Icon(
                            painter = painterResource(
                                id = if (glassBorder) R.drawable.check else R.drawable.close
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // -------------------------------------------------------------
        // PLAYER SLIDER STYLE
        // -------------------------------------------------------------
        Text(
            text = "Player Slider Style",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Visual design for the playback seekbar in the full player",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val sliderStyles = listOf(
            Triple(SliderStyle.DEFAULT, false, "Default"),
            Triple(SliderStyle.WAVY, false, "Wavy"),
            Triple(SliderStyle.WAVY, true, "Squiggly"),
            Triple(SliderStyle.SLIM, false, "Slim")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(sliderStyles) { (style, isSquiggly, label) ->
                val isSelected = (sliderStyle == style) && (style != SliderStyle.WAVY || squigglySlider == isSquiggly)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSliderStyleChange(style)
                        onSquigglySliderChange(isSquiggly)
                    },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // -------------------------------------------------------------
        // AUDIO QUALITY BADGE
        // -------------------------------------------------------------
        Text(
            text = "Audio Information",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowAudioQualityBadgeChange(!showAudioQualityBadge) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Audio Quality Badge",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Display format tags (FLAC, Hi-Res, etc.) on the player screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showAudioQualityBadge,
                    onCheckedChange = onShowAudioQualityBadgeChange,
                    thumbContent = {
                        Icon(
                            painter = painterResource(
                                id = if (showAudioQualityBadge) R.drawable.check else R.drawable.close
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // -------------------------------------------------------------
        // NAVIGATION BAR
        // -------------------------------------------------------------
        Text(
            text = "Navigation Bar",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Docked classic bar or modern floating pill navigation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = floatingNavBar,
                onClick = { onFloatingNavBarChange(true) },
                label = {
                    Text(
                        text = "Floating (Default)",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                leadingIcon = if (floatingNavBar) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            FilterChip(
                modifier = Modifier.weight(1f),
                selected = !floatingNavBar,
                onClick = { onFloatingNavBarChange(false) },
                label = {
                    Text(
                        text = "Docked",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                leadingIcon = if (!floatingNavBar) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSlimNavChange(!slimNav) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Slim Navigation Bar",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hide text labels for a compact, clean navigation look",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = slimNav,
                    onCheckedChange = onSlimNavChange,
                    thumbContent = {
                        Icon(
                            painter = painterResource(
                                id = if (slimNav) R.drawable.check else R.drawable.close
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    TopAppBar(
        title = { Text("Player Appearance") },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back"
                )
            }
        }
    )
}

@Composable
private fun ControlSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
