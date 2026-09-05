/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware

/**
 * Dynamic Album Art Warping Background
 * Inspired by Glassy Music (NanKillBro/glassy-music-nankill) & SimpMusic acrylic styling.
 * Creates an organic breathing, warping blurred backdrop derived from the current playing artwork.
 */
@Composable
fun GlassyWarpBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 75.dp,
    dimAlpha: Float = 0.35f,
    saturation: Float = 1.6f,
    motionSpeed: Float = 1.0f,
    backgroundAlpha: Float = 1.0f,
    content: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val isMotionActive = motionSpeed > 0.05f

    val infiniteTransition = rememberInfiniteTransition(label = "glassyWarpMotion")

    // Breathing scale animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.18f,
        targetValue = 1.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isMotionActive) (14000 / motionSpeed).toInt().coerceAtLeast(1000) else 14000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glassyScale"
    )

    // Gentle swaying liquid rotation
    val liquidRotation by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isMotionActive) (22000 / motionSpeed).toInt().coerceAtLeast(1000) else 22000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glassyRotation"
    )

    // Secondary wave rotation
    val waveRotation by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isMotionActive) (17000 / motionSpeed).toInt().coerceAtLeast(1000) else 17000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveRotation"
    )

    val currentScale = if (isMotionActive) breathingScale else 1.25f
    val currentRotation = if (isMotionActive) liquidRotation else 0f
    val currentWave = if (isMotionActive) waveRotation else 0f

    val colorMatrix = remember(saturation) {
        ColorMatrix().apply {
            setToSaturation(saturation)
        }
    }
    val colorFilter = remember(colorMatrix) { ColorFilter.colorMatrix(colorMatrix) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(backgroundAlpha)
    ) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = {
                fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
            },
            label = "glassyThumbnailTransition"
        ) { url ->
            if (!url.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base Layer: primary breathing blur
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .size(160, 160)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = currentScale
                                scaleY = currentScale
                                rotationZ = currentRotation
                            }
                            .blur(blurRadius)
                    )

                    // Secondary Layer: offset drifting liquid warp
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .size(140, 140)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.55f)
                            .graphicsLayer {
                                scaleX = currentScale * 1.08f
                                scaleY = currentScale * 1.08f
                                rotationZ = currentWave
                            }
                            .blur((blurRadius.value * 1.25f).dp)
                    )

                    // Ambient dim layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 0.9f)))
                    )

                    // Vertical vignette gradient for player contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                }
            } else {
                // Fallback neutral backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF101014))
                )
            }
        }

        content?.invoke()
    }
}
