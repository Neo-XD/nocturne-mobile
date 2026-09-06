/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import android.app.ActivityManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nocturne.music.constants.EnableFrostedGlassKey
import com.nocturne.music.constants.GlassBlurRadiusKey
import com.nocturne.music.ui.component.backdrop.Backdrop
import com.nocturne.music.ui.component.backdrop.BackdropEffectScope
import com.nocturne.music.ui.component.backdrop.drawBackdrop
import com.nocturne.music.ui.component.backdrop.effects.blur
import com.nocturne.music.ui.component.backdrop.effects.colorControls
import com.nocturne.music.ui.component.backdrop.effects.lens
import com.nocturne.music.ui.component.backdrop.highlight.Highlight
import com.nocturne.music.ui.component.backdrop.highlight.HighlightStyle
import com.nocturne.music.ui.component.backdrop.isRenderEffectSupported
import com.nocturne.music.ui.component.backdrop.shadow.Shadow
import com.nocturne.music.utils.rememberPreference

/**
 * User-configurable parameters of the liquid glass effect, sourced from DataStore
 * preferences in MainActivity and distributed through [LocalGlassEffectConfig].
 */
@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = true,
    val vibrancy: Float = 1.2f,
    /** Blur in dp applied to glass elements. User-controlled via settings slider. */
    val blurRadius: Float = 50f,
    /** 0..1, mapped to 0..[LENS_MAX_DP] dp of lens refraction height. 0.4 = 40%. */
    val lensHeight: Float = 0.4f,
    /** 0..1, mapped to 0..[LENS_MAX_DP] dp of lens refraction amount. 0.6 = 60%. */
    val lensAmount: Float = 0.6f,
    val chromaticAberration: Boolean = false,
    val depthEffect: Boolean = false,
    /** [Color.Unspecified] means adaptive. */
    val surfaceTintColor: Color = Color.Unspecified,
    /** Specular rim colour. [Color.Unspecified] keeps the default white. */
    val highlightColor: Color = Color.Unspecified,
    /** Specular rim opacity, 0..1. */
    val highlightOpacity: Float = EdgeHighlightAlpha,
    /** Which rendering style every glass surface uses. */
    val style: GlassStyle = GlassStyle.LIQUID,
    val surfaceOpacity: Float = 0.5f,
    val textColor: Color = Color.White,
    val playerEnabled: Boolean = true,
    val miniPlayerEnabled: Boolean = true,
    val navBarEnabled: Boolean = true,
) {
    fun isEnabledFor(component: GlassComponent): Boolean =
        globalEnabled && when (component) {
            GlassComponent.PLAYER -> playerEnabled
            GlassComponent.MINI_PLAYER -> miniPlayerEnabled
            GlassComponent.NAV_BAR -> navBarEnabled
        }

    val anyComponentEnabled: Boolean
        get() = globalEnabled && (playerEnabled || miniPlayerEnabled || navBarEnabled)
}

enum class GlassStyle {
    /** Blur + saturation + lens refraction + specular rim. The default. */
    LIQUID,

    /** Blur and saturation only: no lens, no rim, no chromatic aberration. Frosted glass. */
    BLUR,

    /** No capture at all — a translucent tinted fill at the configured opacity. */
    TRANSPARENT,
}

fun shouldUseTranslucentGlassFallback(style: GlassStyle, renderEffectSupported: Boolean): Boolean =
    style == GlassStyle.TRANSPARENT || !renderEffectSupported

enum class GlassComponent {
    PLAYER,
    MINI_PLAYER,
    NAV_BAR,
}

internal const val LENS_MAX_DP = 48f
internal const val PLAYER_BLUR_MULTIPLIER = 2f
internal const val MIN_GLASS_RESOLUTION_SCALE = 0.30f
internal const val FULL_QUALITY_BLUR_DP = 8f

fun glassResolutionScale(blurRadiusDp: Float): Float {
    val t = (blurRadiusDp / FULL_QUALITY_BLUR_DP).coerceIn(0f, 1f)
    return 1f - t * (1f - MIN_GLASS_RESOLUTION_SCALE)
}

fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S

@Composable
fun isLowRamDevice(): Boolean {
    val context = LocalContext.current
    return remember {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false
    }
}

@Composable
fun isGlassAllowed(): Boolean = isGlassSupported() && !isLowRamDevice()

fun glassSaturation(vibrancy: Float): Float = 1f + 0.5f * vibrancy.coerceIn(0f, 2f)

fun glassContentColorFor(behind: Color, tint: Color, opacity: Float): Color {
    val effective = if (tint.isSpecified) {
        lerp(behind, tint, opacity.coerceIn(0f, 1f))
    } else {
        behind
    }
    return if (effective.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
}

private val EdgeHighlightWidth = 0.8f.dp
private const val EdgeHighlightAlpha = 0.55f
private const val HighlightAngleFrozen = 45f

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }

/** The backdrop content (app UI) that glass surfaces sample from. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val LocalBackdropLoopBucket = staticCompositionLocalOf<(() -> Int)?> { null }

@Composable
fun rememberGlassEffectConfig(): GlassEffectConfig {
    val (enabled) = rememberPreference(EnableFrostedGlassKey, defaultValue = true)
    val (blurRadius) = rememberPreference(GlassBlurRadiusKey, defaultValue = 50f)
    return remember(enabled, blurRadius) {
        GlassEffectConfig(
            globalEnabled = enabled,
            blurRadius = blurRadius,
            style = GlassStyle.LIQUID
        )
    }
}

/**
 * Renders this composable as a liquid glass surface sampling [LocalAppBackdrop].
 */
@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    blurRadiusDp: Float = config.blurRadius,
    highlightAlpha: Float = EdgeHighlightAlpha,
    backdropScale: Float = glassResolutionScale(blurRadiusDp),
    frozen: () -> Boolean = { false },
    loopBucket: (() -> Int)? = LocalBackdropLoopBucket.current,
): Modifier {
    if (!isGlassAllowed() || !config.globalEnabled) return this
    val backdrop = LocalAppBackdrop.current ?: return this
    val density = LocalDensity.current
    val resolutionScale = backdropScale.coerceIn(0.05f, 1f)
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * resolutionScale
    val saturation = glassSaturation(config.vibrancy)
    val lensHeightPx = with(density) { (config.lensHeight * LENS_MAX_DP).dp.toPx() } * resolutionScale
    val lensAmountPx = with(density) { (config.lensAmount * LENS_MAX_DP).dp.toPx() } * resolutionScale
    
    val surfaceTintColor = if (config.surfaceTintColor.isSpecified) {
        config.surfaceTintColor
    } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF2C2C2E)
    }

    if (shouldUseTranslucentGlassFallback(config.style, isRenderEffectSupported())) {
        return this
            .clip(shape)
            .background(surfaceTintColor.copy(alpha = config.surfaceOpacity.coerceIn(0f, 1f)))
    }

    val shapeBlock: () -> Shape = remember(shape) { { shape } }
    val plainBlur = config.style == GlassStyle.BLUR

    val effectsBlock: BackdropEffectScope.() -> Unit = remember(
        saturation,
        blurPx,
        applyEdgeEffects,
        plainBlur,
        lensHeightPx,
        lensAmountPx,
        config.depthEffect,
        config.chromaticAberration,
    ) {
        {
            if (saturation != 1f) {
                colorControls(saturation = saturation)
            }
            if (blurPx > 0f) {
                blur(blurPx)
            }
            if (!plainBlur &&
                applyEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        }
    }

    val rimColor = if (config.highlightColor.isSpecified) config.highlightColor else Color.White
    val rimAlpha = (highlightAlpha * (config.highlightOpacity / EdgeHighlightAlpha)).coerceIn(0f, 1f)

    val highlightBlock: (() -> Highlight?)? = remember(applyEdgeEffects, plainBlur, rimColor, rimAlpha) {
        if (applyEdgeEffects && !plainBlur) {
            {
                Highlight(
                    width = EdgeHighlightWidth,
                    style = HighlightStyle.Default(
                        color = rimColor.copy(alpha = rimAlpha),
                        angle = HighlightAngleFrozen,
                    ),
                )
            }
        } else {
            null
        }
    }

    val shadowBlock: (() -> Shadow?)? = remember(applyEdgeEffects) {
        if (applyEdgeEffects) ({ Shadow.Default }) else null
    }

    val surfaceBlock: DrawScope.() -> Unit = remember(surfaceTintColor, config.surfaceOpacity) {
        {
            if (config.surfaceOpacity > 0f) {
                drawRect(
                    color = surfaceTintColor.copy(alpha = config.surfaceOpacity),
                    size = size,
                )
            }
        }
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = shapeBlock,
        effects = effectsBlock,
        highlight = highlightBlock,
        shadow = shadowBlock,
        onDrawSurface = surfaceBlock,
        backdropScale = resolutionScale,
        frozen = frozen,
        loopBucket = loopBucket,
    )
}
