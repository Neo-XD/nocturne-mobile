/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.nocturne.music.constants.GlassBlurRadiusKey
import com.nocturne.music.utils.rememberPreference

/**
 * Global haze state for frosted glass blur.
 * The main content area provides this as a source; navbars + miniplayer consume it.
 */
val LocalHazeState = compositionLocalOf { HazeState() }

/**
 * Standard Nocturne Specular Border gradient creating the acrylic glass edge reflection.
 */
@Composable
fun rememberNocturneGlassBorderBrush(
    highlightAlpha: Float = 0.32f,
    shadowAlpha: Float = 0.08f
): Brush {
    return remember(highlightAlpha, shadowAlpha) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = highlightAlpha),
                Color.White.copy(alpha = shadowAlpha),
                Color.Transparent,
                Color.White.copy(alpha = highlightAlpha * 0.4f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
}

/**
 * Returns the standard Nocturne glass HazeStyle used everywhere.
 * Blur radius is user-configurable via the Appearance settings slider (default 50dp).
 */
@Composable
fun rememberNocturneHazeStyle(
    tintAlpha: Float = 0.15f,
    noiseFactor: Float = 0.08f
): HazeStyle {
    val (blurRadiusPref) = rememberPreference(GlassBlurRadiusKey, defaultValue = 50f)
    val blurRadius = blurRadiusPref.dp
    val tintColor = MaterialTheme.colorScheme.surface.copy(alpha = tintAlpha)
    return remember(blurRadius, tintAlpha, noiseFactor) {
        HazeStyle(
            blurRadius = blurRadius,
            tint = HazeTint(tintColor),
            noiseFactor = noiseFactor
        )
    }
}

/**
 * Nocturne Frosted Glass Modifier
 * Applies a translucent acrylic frosted layer with optional specular glass border.
 */
fun Modifier.nocturneFrostedGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.Unspecified,
    showBorder: Boolean = true,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .then(
        if (showBorder && borderBrush != null) {
            Modifier.border(borderWidth, borderBrush, shape)
        } else {
            Modifier
        }
    )
    .background(backgroundColor, shape)

/**
 * High-performance frosted glass container card.
 */
@Composable
fun NocturneGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderBrush: Brush = rememberNocturneGlassBorderBrush(),
    showBorder: Boolean = true,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.then(
            if (showBorder) {
                Modifier.border(BorderStroke(1.dp, borderBrush), shape)
            } else {
                Modifier
            }
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = elevation
    ) {
        content()
    }
}

/**
 * High-performance frosted glass container box.
 * Uses backdrop liquid glass for real backdrop blur.
 */
@Composable
fun NocturneGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    showBorder: Boolean = true,
    borderBrush: Brush = rememberNocturneGlassBorderBrush(),
    content: @Composable BoxScope.() -> Unit
) {
    val cornerShape = shape as? androidx.compose.foundation.shape.CornerBasedShape ?: RoundedCornerShape(24.dp)
    val glassConfig = LocalGlassEffectConfig.current
    val backdrop = LocalAppBackdrop.current
    
    val glassModifier = if (backdrop != null && glassConfig.globalEnabled && isGlassAllowed()) {
        Modifier
            .liquidGlass(
                config = glassConfig,
                shape = cornerShape,
                applyEdgeEffects = false
            )
            .then(
                if (showBorder) {
                    Modifier.border(BorderStroke(1.dp, borderBrush), shape)
                } else {
                    Modifier
                }
            )
    } else {
        val hazeState = LocalHazeState.current
        val hazeStyle = rememberNocturneHazeStyle()
        Modifier
            .clip(shape)
            .hazeEffect(state = hazeState, style = hazeStyle)
            .then(
                if (showBorder) {
                    Modifier.border(BorderStroke(1.dp, borderBrush), shape)
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, shape)
    }
    Box(
        modifier = modifier.then(glassModifier),
        content = content
    )
}
