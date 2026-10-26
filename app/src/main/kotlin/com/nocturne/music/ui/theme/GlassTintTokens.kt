/**
 * Convx / Nocturne Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

fun Color.isHueless(): Boolean {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    return hsl[1] < 0.08f
}

/** Theme and component-specific glass color tokens and calculations. */
object GlassTintTokens {
    // §1. Base Surface Tokens
    val SurfaceBaseDark = Color(0xFF171717)        // #171717, rgb: 23, 23, 23
    val SurfaceBaseLight = Color(0xFFF5F5F7)       // #F5F5F7, rgb: 245, 245, 247

    // §3.3. Neutral Tint Tokens (Fallback / non-hero screens)
    val TintNeutralDark = Color(0xFFEBEBF5).copy(alpha = 0f)
    val TintNeutralLight = Color(0xFF3C3C43).copy(alpha = 0.05f)

    // §3.2. Hero Tint Opacity
    const val HeroTintOpacityDark = 0f
    const val HeroTintOpacityLight = 0.25f

    // §4.4. Accent Selected Red Tokens (iOS systemRed)
    val AccentSelectedRedDark = Color(0xFFFF453A)  // #FF453A, dark mode
    val AccentSelectedRedLight = Color(0xFFFF3B30) // #FF3B30, light mode

    // Light Mode Opaque Blackish Tokens
    val IconOpaqueBlackishLight = Color(0xFF111111)
    val TitleOpaqueBlackishLight = Color(0xFF000000)
    val TabLabelOpaqueBlackishLight = Color(0xFF1C1C1E)

    // Specular Rim / Pluck Tokens
    val PluckDefaultDark = Color(0xFFFFFFFF)
    val PluckDefaultLight = Color(0xFFFFFFFF)
    const val PluckAlphaNeutralDark = 0.30f
    const val PluckAlphaNeutralLight = 0.40f
    const val PluckAlphaHeroDark = 0.48f
    const val PluckAlphaHeroLight = 0.55f

    // §2 & §5B. Border / Stroke Tokens
    val BorderDark = Color(0xFFFFFFFF).copy(alpha = 0.12f)
    val BorderLight = Color(0xFF000000).copy(alpha = 0.08f)

    // §4.1 & §5B. Dim Opacity Values
    const val IconDimOpacity = 0.85f
    const val TitleDimOpacity = 0.92f
    const val UnselectedTabLabelOpacity = 0.75f
    const val SelectedTabLabelOpacity = 0.92f
    const val DragHandleOpacity = 0.30f
    const val SecondaryLabelAlpha = 0.60f

    // Per-theme glass defaults.
    val DefaultSurfaceDark = Color(0xFF171717)
    const val DefaultSurfaceOpacityDark = 0.50f
    val DefaultPuckDark = Color(0xFF2E2E2E)
    const val DefaultPuckOpacityDark = 0.80f
    val DefaultHighlightDark = Color(0xFFE8E8E8)
    const val DefaultHighlightOpacityDark = 0.33f
    val DefaultSurfaceLight = Color(0xFFFBFBFB)
    const val DefaultSurfaceOpacityLight = 0.40f
    val DefaultPuckLight = Color(0xFFD1D1D1)
    const val DefaultPuckOpacityLight = 0.67f
    val DefaultHighlightLight = Color.White
    const val DefaultHighlightOpacityLight = 0.60f

    // Nav-bar glass physics
    const val NavVibrancy = 2.0f
    const val NavBlurDp = 3f
    const val NavLensHeight = 0.60f
    const val NavLensAmount = 0.69f
    const val OtherLensHeight = 0.35f
    const val OtherLensAmount = 0.40f

    // Base Surface Opacities: white lift in dark (+15%)
    const val BaseSurfaceOpacityDark = 0.15f
    const val BaseSurfaceOpacityLight = 0.40f

    const val NavPillFillOpacityDark = 0.55f
    const val NavPillFillOpacityLight = 0.68f

    const val SearchFieldFillOpacityDark = 0.60f
    const val SearchFieldFillOpacityLight = 0.72f

    const val PillSelectedFillOpacityDark = 0.70f
    const val PillSelectedFillOpacityLight = 0.80f

    val NavBarBlurDark: Dp = 3.dp
    val NavBarBlurLight: Dp = 3.dp
    val TabBarBlurDark: Dp = 3.dp
    val TabBarBlurLight: Dp = 3.dp
    val SheetBlurDark: Dp = 14.dp
    val SheetBlurLight: Dp = 12.dp

    const val NavBarSaturationDark = 1.70f
    const val NavBarSaturationLight = 1.50f

    val ShadowColorDark = Color(0x59000000)
    val ShadowColorLight = Color(0x1F000000)

    /** Base of a live liquid-glass surface: white lift in dark, #F5F5F7 in light. */
    fun glassBaseColor(isDark: Boolean): Color = if (isDark) Color.White else SurfaceBaseLight

    fun baseSurfaceColor(isDark: Boolean): Color =
        if (isDark) SurfaceBaseDark else SurfaceBaseLight

    fun neutralTintColor(isDark: Boolean): Color =
        if (isDark) TintNeutralDark else TintNeutralLight

    fun heroTintOpacity(isDark: Boolean): Float =
        if (isDark) HeroTintOpacityDark else HeroTintOpacityLight

    fun selectedRedColor(isDark: Boolean): Color =
        if (isDark) AccentSelectedRedDark else AccentSelectedRedLight

    fun borderColor(isDark: Boolean): Color =
        if (isDark) BorderDark else BorderLight

    fun navBarBlur(isDark: Boolean): Dp =
        if (isDark) NavBarBlurDark else NavBarBlurLight

    fun tabBarBlur(isDark: Boolean): Dp =
        if (isDark) TabBarBlurDark else TabBarBlurLight

    fun navBarSaturation(isDark: Boolean): Float =
        if (isDark) NavBarSaturationDark else NavBarSaturationLight

    /** Returns the specular highlight ("pluck") rim color adapted to the theme and artwork. */
    fun pluckColor(
        isDark: Boolean,
        ambientColor: Color = Color.Unspecified,
        rimTintColor: Color = Color.Unspecified,
    ): Color {
        return when {
            rimTintColor.isSpecified -> rimTintColor
            ambientColor.isSpecified && !ambientColor.isHueless() -> {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(ambientColor.toArgb(), hsl)
                hsl[1] = hsl[1].coerceIn(0.40f, 0.55f)
                hsl[2] = if (isDark) hsl[2].coerceIn(0.62f, 0.74f) else hsl[2].coerceIn(0.75f, 0.90f)
                Color(ColorUtils.HSLToColor(hsl))
            }
            else -> if (isDark) PluckDefaultDark else PluckDefaultLight
        }
    }

    fun pluckOpacity(isDark: Boolean, isHeroContext: Boolean): Float {
        return if (isDark) {
            if (isHeroContext) PluckAlphaHeroDark else PluckAlphaNeutralDark
        } else {
            if (isHeroContext) PluckAlphaHeroLight else PluckAlphaNeutralLight
        }
    }

    fun puckOpacity(isDark: Boolean, isHeroContext: Boolean): Float {
        return if (isDark) {
            if (isHeroContext) 0.80f else 0.55f
        } else {
            if (isHeroContext) 0.70f else 0.45f
        }
    }

    /** Selected content uses red; light mode uses opaque black, while dark mode adapts to [activeTint]. */
    fun glassContentColor(
        isDark: Boolean,
        activeTint: Color = Color.Unspecified,
        isSelected: Boolean = false,
        isTitle: Boolean = false,
        isUnselectedTabLabel: Boolean = false,
        overrideRedForSelection: Boolean = true,
    ): Color {
        if (isSelected && overrideRedForSelection) {
            val red = selectedRedColor(isDark)
            val alpha = if (isDark) {
                if (isTitle || isUnselectedTabLabel) SelectedTabLabelOpacity else IconDimOpacity
            } else {
                1.0f
            }
            return red.copy(alpha = alpha)
        }

        if (!isDark) {
            return when {
                isTitle -> TitleOpaqueBlackishLight
                isUnselectedTabLabel -> TabLabelOpaqueBlackishLight
                else -> IconOpaqueBlackishLight
            }
        }

        val dimAlpha = when {
            isTitle -> TitleDimOpacity
            isUnselectedTabLabel -> UnselectedTabLabelOpacity
            else -> IconDimOpacity
        }

        val effectiveTint = if (activeTint.isSpecified && !activeTint.isHueless()) {
            activeTint
        } else {
            neutralTintColor(isDark = true)
        }

        val shifted = if (effectiveTint.isSpecified && !effectiveTint.isHueless()) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(effectiveTint.toArgb(), hsl)
            val targetHsl = FloatArray(3)
            targetHsl[0] = hsl[0]
            targetHsl[1] = hsl[1].coerceIn(0.18f, 0.38f)
            targetHsl[2] = if (isTitle) 0.96f else 0.92f
            Color(ColorUtils.HSLToColor(targetHsl))
        } else {
            if (isTitle) Color(0xFFFFFFFF) else Color(0xFFEBEBF5)
        }

        return shifted.copy(alpha = dimAlpha)
    }
}
