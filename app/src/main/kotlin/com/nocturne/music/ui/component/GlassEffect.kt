/**
 * Convx / Nocturne Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import android.app.ActivityManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import com.nocturne.music.constants.EnableFrostedGlassKey
import com.nocturne.music.constants.GlassBlurRadiusKey
import com.nocturne.music.constants.GlassPerformanceKey
import com.nocturne.music.constants.GlassChromaticAberrationKey
import com.nocturne.music.constants.GlassDepthEffectKey
import com.nocturne.music.constants.GlassHighlightOpacityKey
import com.nocturne.music.constants.GlassLensAmountKey
import com.nocturne.music.constants.GlassLensHeightKey
import com.nocturne.music.constants.GlassSurfaceOpacityKey
import com.nocturne.music.constants.GlassVibrancyKey
import com.nocturne.music.ui.component.backdrop.Backdrop
import com.nocturne.music.ui.component.backdrop.BackdropEffectScope
import com.nocturne.music.ui.component.backdrop.backdrops.LayerBackdrop
import com.nocturne.music.ui.component.backdrop.drawBackdrop
import com.nocturne.music.ui.component.backdrop.effects.blur
import com.nocturne.music.ui.component.backdrop.effects.colorControls
import com.nocturne.music.ui.component.backdrop.effects.lens
import com.nocturne.music.ui.component.backdrop.poolLens
import com.nocturne.music.ui.component.backdrop.highlight.Highlight
import com.nocturne.music.ui.component.backdrop.highlight.HighlightStyle
import com.nocturne.music.ui.component.backdrop.isRenderEffectSupported
import com.nocturne.music.ui.component.backdrop.shadow.Shadow
import com.nocturne.music.ui.theme.GlassTintTokens
import com.nocturne.music.ui.theme.isHueless
import com.nocturne.music.ui.utils.outerDarkRim
import com.nocturne.music.utils.rememberPreference

/**
 * User-configurable parameters of the liquid glass effect, sourced from DataStore
 * preferences in MainActivity and distributed through [LocalGlassEffectConfig].
 * 1:1 with Convx's high-performance pooled liquid glass pipeline.
 */
@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = true,
    val vibrancy: Float = 2.0f,
    /** Blur in dp applied to glass elements. Convx spec = 3dp. */
    val blurRadius: Float = 3f,
    /** 0..1, mapped to 0..[LENS_MAX_DP] dp of lens refraction height. */
    val lensHeight: Float = 0.60f,
    /** 0..1, mapped to 0..[LENS_MAX_DP] dp of lens refraction amount. */
    val lensAmount: Float = 0.69f,
    val chromaticAberration: Boolean = false,
    val depthEffect: Boolean = false,
    /** [Color.Unspecified] means adaptive: derived from the theme surface. */
    val surfaceTintColor: Color = Color.Unspecified,
    /** Specular rim colour. [Color.Unspecified] keeps the default white. */
    val highlightColor: Color = Color.Unspecified,
    /** Specular rim opacity, 0..1. */
    val highlightOpacity: Float = EdgeHighlightAlpha,
    /** Which rendering style every glass surface uses. */
    val style: GlassStyle = GlassStyle.LIQUID,
    val surfaceOpacity: Float = 0.50f,
    val glassPerformance: Boolean = false,
    val textColor: Color = Color.Unspecified,
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
internal const val PLAYER_BLUR_MULTIPLIER = 4f
internal const val MIN_GLASS_RESOLUTION_SCALE = 0.30f
internal const val FULL_QUALITY_BLUR_DP = 2.5f
private const val FROST_BLUR_RADIUS_DP = 16f
private const val FROST_RESOLUTION_SCALE = 0.25f

val GLASS_EDGE_WIDTH = 0.5.dp
val GLASS_EDGE_COLOR = Color.White.copy(alpha = 0.10f)

@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }

@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier {
    val config = LocalGlassEffectConfig.current
    val useGlass = config.globalEnabled && isGlassSupported()
    val glassTint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }

    return clip(shape)
        .background(
            color = if (useGlass) glassTint.copy(alpha = 0.4f) else fallbackColor,
            shape = shape,
        )
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

@Composable
fun Modifier.liquidGlassRim(shape: CornerBasedShape): Modifier {
    val config = LocalGlassEffectConfig.current
    if (!config.globalEnabled || !isGlassSupported()) return this
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val rimColor = GlassTintTokens.pluckColor(isDark).copy(alpha = 0.4f)
    return border(GLASS_EDGE_WIDTH, rimColor, shape)
}

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
internal const val HighlightAngleFrozen = 45f
private const val GlassThemeFadeMillis = 280
private val DarkRimWidth = 1.6.dp
private const val DarkRimAlpha = 0.32f

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }

/** The backdrop content (app UI) that glass surfaces sample from. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop> { error("No AppBackdrop provided") }

/** Processed backdrop shared by surfaces with matching blur and vibrancy settings. */
@Stable
class PooledGlass(
    val backdrop: LayerBackdrop,
    val blurRadiusDp: Float,
    val vibrancy: Float,
)

val LocalPooledGlass = staticCompositionLocalOf<PooledGlass?> { null }

val LocalAppBackdropFrozen = staticCompositionLocalOf<() -> Boolean> { { false } }

val LocalAppMotionFrozen = staticCompositionLocalOf<State<Boolean>> {
    object : State<Boolean> {
        override val value: Boolean get() = false
    }
}

val LocalBackdropLoopBucket = staticCompositionLocalOf<(() -> Int)?> { null }

@Composable
fun rememberGlassEffectConfig(): GlassEffectConfig {
    val (enabled) = rememberPreference(EnableFrostedGlassKey, defaultValue = true)
    val (blurRadius) = rememberPreference(GlassBlurRadiusKey, defaultValue = 3f)
    val (vibrancy) = rememberPreference(GlassVibrancyKey, defaultValue = 2.0f)
    val (highlightOpacity) = rememberPreference(GlassHighlightOpacityKey, defaultValue = EdgeHighlightAlpha)
    val (lensAmount) = rememberPreference(GlassLensAmountKey, defaultValue = 0.69f)
    val (lensHeight) = rememberPreference(GlassLensHeightKey, defaultValue = 0.60f)
    val (chromaticAberration) = rememberPreference(GlassChromaticAberrationKey, defaultValue = false)
    val (depthEffect) = rememberPreference(GlassDepthEffectKey, defaultValue = false)
    val (surfaceOpacity) = rememberPreference(GlassSurfaceOpacityKey, defaultValue = 0.50f)
    val (glassPerformance) = rememberPreference(GlassPerformanceKey, defaultValue = false)

    return remember(
        enabled,
        blurRadius,
        vibrancy,
        highlightOpacity,
        lensAmount,
        lensHeight,
        chromaticAberration,
        depthEffect,
        surfaceOpacity,
        glassPerformance
    ) {
        GlassEffectConfig(
            globalEnabled = enabled,
            blurRadius = blurRadius,
            vibrancy = vibrancy,
            highlightOpacity = highlightOpacity,
            lensAmount = lensAmount,
            lensHeight = lensHeight,
            chromaticAberration = chromaticAberration,
            depthEffect = depthEffect,
            surfaceOpacity = surfaceOpacity,
            glassPerformance = glassPerformance,
            style = if (glassPerformance) GlassStyle.BLUR else GlassStyle.LIQUID
        )
    }
}

/**
 * Draws a glass surface from [LocalPooledGlass] or [LocalAppBackdrop], applying configured blur,
 * vibrancy, tint, and edge refraction. 1:1 with Convx's high-performance implementation.
 */
@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    blurRadiusDp: Float = config.blurRadius,
    highlightAlpha: Float = EdgeHighlightAlpha,
    backdropScale: Float = glassResolutionScale(blurRadiusDp),
    frozen: () -> Boolean = LocalAppBackdropFrozen.current,
    loopBucket: (() -> Int)? = LocalBackdropLoopBucket.current,
    rimTintColor: Color = Color.Unspecified,
    outerShadow: Boolean = true,
): Modifier {
    val isAllowed = isGlassAllowed()
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f

    if (!isAllowed || !config.globalEnabled) {
        val base = GlassTintTokens.glassBaseColor(isDark)
        val tint = config.surfaceTintColor.takeIf { it.isSpecified }
        return this.clip(shape)
            .background(base.copy(alpha = 0.85f))
            .then(if (tint != null) Modifier.background(tint.copy(alpha = config.surfaceOpacity.coerceIn(0f, 1f))) else Modifier)
    }

    val pool = LocalPooledGlass.current
    val isPerformance = config.glassPerformance
    val usePool = !isPerformance && pool != null &&
        blurRadiusDp == pool.blurRadiusDp &&
        config.vibrancy == pool.vibrancy &&
        !config.chromaticAberration
    val backdrop = if (usePool) pool.backdrop else LocalAppBackdrop.current
    val density = LocalDensity.current
    val effectiveResolutionScale = if (isPerformance) FROST_RESOLUTION_SCALE else (if (usePool) 1f else backdropScale.coerceIn(0.05f, 1f))
    val effectiveBlurRadiusDp = if (isPerformance) FROST_BLUR_RADIUS_DP else blurRadiusDp
    val resolutionScale = effectiveResolutionScale
    val blurPx = with(density) { effectiveBlurRadiusDp.dp.toPx() } * resolutionScale
    val saturation = glassSaturation(config.vibrancy)
    val lensHeightPx = with(density) { (config.lensHeight * LENS_MAX_DP).dp.toPx() } * resolutionScale
    val lensAmountPx = with(density) { (config.lensAmount * LENS_MAX_DP).dp.toPx() } * resolutionScale

    val baseSurfaceColor = GlassTintTokens.glassBaseColor(isDark)
    val baseSurfaceOpacity = if (config.surfaceTintColor.isSpecified) 0f else if (isDark) {
        GlassTintTokens.BaseSurfaceOpacityDark
    } else {
        GlassTintTokens.BaseSurfaceOpacityLight
    }

    val tintColor = when {
        config.surfaceTintColor.isSpecified -> config.surfaceTintColor
        else -> GlassTintTokens.neutralTintColor(isDark)
    }
    val tintOpacity = when {
        config.surfaceTintColor.isSpecified -> config.surfaceOpacity.coerceIn(0f, 1f)
        else -> 1f
    }
    val effectiveTintAlpha = (tintColor.alpha * tintOpacity).coerceIn(0f, 1f)

    val glassFade = tween<Color>(GlassThemeFadeMillis)
    val animBaseColor = animateColorAsState(baseSurfaceColor.copy(alpha = baseSurfaceOpacity), glassFade, label = "glassBase")
    val animTintColor = animateColorAsState(tintColor.copy(alpha = effectiveTintAlpha), glassFade, label = "glassTint")

    if (shouldUseTranslucentGlassFallback(config.style, isRenderEffectSupported())) {
        return this
            .clip(shape)
            .background(baseSurfaceColor.copy(alpha = baseSurfaceOpacity))
            .background(tintColor.copy(alpha = effectiveTintAlpha))
    }

    val animatedHighlightAngle = HighlightAngleFrozen
    val shapeBlock: () -> Shape = remember(shape) { { shape } }
    val plainBlur = config.style == GlassStyle.BLUR || isPerformance

    val effectsBlock: BackdropEffectScope.() -> Unit = remember(
        saturation,
        blurPx,
        applyEdgeEffects,
        plainBlur,
        lensHeightPx,
        lensAmountPx,
        config.depthEffect,
        config.chromaticAberration,
        usePool,
    ) {
        {
            if (!usePool && saturation != 1f) {
                colorControls(saturation = saturation)
            }
            if (!usePool && blurPx > 0f) {
                blur(blurPx)
            }
            if (!plainBlur &&
                applyEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                if (usePool) {
                    poolLens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = config.depthEffect,
                    )
                } else {
                    lens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = config.depthEffect,
                        chromaticAberration = config.chromaticAberration,
                    )
                }
            }
        }
    }

    val rimColor = when {
        config.highlightColor.isSpecified -> config.highlightColor
        rimTintColor.isSpecified -> rimTintColor
        else -> GlassTintTokens.pluckColor(isDark)
    }
    val baseHighlightAlpha = when {
        highlightAlpha != EdgeHighlightAlpha -> highlightAlpha
        else -> GlassTintTokens.pluckOpacity(isDark, false)
    }
    val rimAlpha = (baseHighlightAlpha * (config.highlightOpacity / EdgeHighlightAlpha)).coerceIn(0f, 1f)

    val animRim = animateColorAsState(rimColor.copy(alpha = rimAlpha), glassFade, label = "glassRim")
    val highlightBlock: (() -> Highlight?)? = remember(applyEdgeEffects, plainBlur, rimTintColor, animatedHighlightAngle) {
        if (applyEdgeEffects && !plainBlur) {
            {
                Highlight(
                    width = EdgeHighlightWidth,
                    style = HighlightStyle.Default(
                        color = animRim.value,
                        angle = animatedHighlightAngle,
                    ),
                )
            }
        } else {
            null
        }
    }

    val shadowBlock: (() -> Shadow?)? = remember(applyEdgeEffects, usePool, outerShadow) {
        if (applyEdgeEffects && !usePool && outerShadow) ({ Shadow.Default }) else null
    }

    val darkRimAlpha = (DarkRimAlpha * (config.highlightOpacity / EdgeHighlightAlpha)).coerceIn(0f, 1f)

    val surfaceBlock: DrawScope.() -> Unit = remember(usePool, shape) {
        {
            val outline = if (usePool) shape.createOutline(size, layoutDirection, this) else null
            val base = animBaseColor.value
            if (base.alpha > 0f) {
                if (outline != null) drawOutline(outline, base) else drawRect(color = base, size = size)
            }
            val tint = animTintColor.value
            if (tint.alpha > 0f) {
                if (outline != null) drawOutline(outline, tint) else drawRect(color = tint, size = size)
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
    ).then(
        if (applyEdgeEffects && !plainBlur && darkRimAlpha > 0f) {
            Modifier.outerDarkRim(
                shape = shape,
                width = DarkRimWidth,
                alpha = darkRimAlpha,
                angle = animatedHighlightAngle,
                insideOnly = usePool,
            )
        } else {
            Modifier
        }
    )
}
