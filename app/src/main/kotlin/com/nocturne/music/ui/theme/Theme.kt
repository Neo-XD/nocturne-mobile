/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

import androidx.compose.runtime.getValue
import com.nocturne.music.constants.SelectedFontKey
import com.nocturne.music.constants.AppFont
import com.nocturne.music.utils.rememberPreference
import androidx.compose.ui.text.font.FontFamily

import com.nocturne.music.constants.ThemeCategory
import com.nocturne.music.constants.ThemeCategoryKey
import com.nocturne.music.constants.NocturneThemePreset
import com.nocturne.music.constants.NocturneThemePresetKey

val DefaultThemeColor = Color(0xFFED5564)

fun buildNocturneDesktopColorScheme(
    preset: NocturneThemePreset,
    darkTheme: Boolean,
    pureBlack: Boolean
): ColorScheme {
    val accent = Color(preset.accentHex)
    val onAccent = if (preset == NocturneThemePreset.LIME || preset == NocturneThemePreset.TEAL) Color(0xFF131314) else Color.White

    return if (darkTheme) {
        val bg = if (pureBlack) Color.Black else Color(0xFF131314)
        val surface = if (pureBlack) Color.Black else Color(0xFF171719)
        val card = if (pureBlack) Color(0xFF101012) else Color(0xFF1E1E22)
        val popover = if (pureBlack) Color(0xFF1A1A1E) else Color(0xFF26262B)
        val secondary = Color(0xFF2E2E33)
        
        ColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = Color(0xFFF5F5F7),
            inversePrimary = Color(0xFF131314),
            secondary = secondary,
            onSecondary = Color(0xFFF5F5F7),
            secondaryContainer = card,
            onSecondaryContainer = Color(0xFFF5F5F7),
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = accent.copy(alpha = 0.15f),
            onTertiaryContainer = Color(0xFFF5F5F7),
            background = bg,
            onBackground = Color(0xFFF5F5F7),
            surface = surface,
            onSurface = Color(0xFFF5F5F7),
            surfaceVariant = secondary,
            onSurfaceVariant = Color(0xFF98989F),
            surfaceTint = accent,
            inverseSurface = Color(0xFFF5F5F7),
            inverseOnSurface = Color(0xFF131314),
            error = Color(0xFFF87171),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFECACA),
            outline = Color(0x28FFFFFF),
            outlineVariant = Color(0x1AFFFFFF),
            scrim = Color(0xBF000000),
            surfaceBright = popover,
            surfaceContainer = card,
            surfaceContainerHigh = popover,
            surfaceContainerHighest = Color(0xFF323238),
            surfaceContainerLow = Color(0xFF161618),
            surfaceContainerLowest = Color(0xFF0F0F10),
            surfaceDim = bg
        )
    } else {
        val bg = Color(0xFFFAFAFA)
        val surface = Color(0xFFFFFFFF)
        val card = Color(0xFFF3F3F5)
        val popover = Color(0xFFEBEBEF)
        val secondary = Color(0xFFE4E4E8)

        ColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.12f),
            onPrimaryContainer = Color(0xFF1C1C1E),
            inversePrimary = Color(0xFFF5F5F7),
            secondary = secondary,
            onSecondary = Color(0xFF1C1C1E),
            secondaryContainer = card,
            onSecondaryContainer = Color(0xFF1C1C1E),
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = accent.copy(alpha = 0.1f),
            onTertiaryContainer = Color(0xFF1C1C1E),
            background = bg,
            onBackground = Color(0xFF1C1C1E),
            surface = surface,
            onSurface = Color(0xFF1C1C1E),
            surfaceVariant = secondary,
            onSurfaceVariant = Color(0xFF6B6B75),
            surfaceTint = accent,
            inverseSurface = Color(0xFF1C1C1E),
            inverseOnSurface = Color(0xFFFAFAFA),
            error = Color(0xFFDC2626),
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B),
            outline = Color(0x1F000000),
            outlineVariant = Color(0x12000000),
            scrim = Color(0x80000000),
            surfaceBright = Color.White,
            surfaceContainer = card,
            surfaceContainerHigh = popover,
            surfaceContainerHighest = Color(0xFFE2E2E6),
            surfaceContainerLow = Color(0xFFF7F7F9),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFECECEF)
        )
    }
}

@Composable
fun NocturneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val selectedFontValue by rememberPreference(SelectedFontKey, AppFont.SYSTEM.value)

    val (themeCategoryStr) = rememberPreference(ThemeCategoryKey, ThemeCategory.NOCTURNE_UI.name)
    val themeCategory = remember(themeCategoryStr) {
        runCatching { ThemeCategory.valueOf(themeCategoryStr) }.getOrDefault(ThemeCategory.NOCTURNE_UI)
    }
    val (nocturnePresetStr) = rememberPreference(NocturneThemePresetKey, NocturneThemePreset.ROSE.name)
    val nocturnePreset = remember(nocturnePresetStr) {
        NocturneThemePreset.fromName(nocturnePresetStr)
    }

    val brandFont = remember(selectedFontValue, themeCategory) {
        val font = AppFont.fromValue(selectedFontValue)
        when {
            font == AppFont.SYSTEM && themeCategory == ThemeCategory.NOCTURNE_UI -> OutfitFontFamily
            font == AppFont.SYSTEM -> FontFamily.Default
            font == AppFont.GOOGLE_SANS -> GoogleSansFontFamily
            font == AppFont.SANS_FLEX -> SansFlexFontFamily
            font == AppFont.OUTFIT -> OutfitFontFamily
            font == AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
            else -> if (themeCategory == ThemeCategory.NOCTURNE_UI) OutfitFontFamily else FontFamily.Default
        }
    }

    val typography = remember(brandFont) {
        getTypography(brandFont = brandFont, plainFont = brandFont)
    }

    val colorScheme = remember(themeCategory, nocturnePreset, darkTheme, pureBlack, themeColor) {
        if (themeCategory == ThemeCategory.NOCTURNE_UI) {
            buildNocturneDesktopColorScheme(nocturnePreset, darkTheme, pureBlack)
        } else {
            // Material 3 Expressive
            val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            val base = if (useSystemDynamicColor) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                com.materialkolor.dynamicColorScheme(
                    seedColor = themeColor,
                    isDark = darkTheme,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                    style = if (themeColor.toArgb() == 0xFF000000.toInt()) PaletteStyle.Monochrome else PaletteStyle.TonalSpot
                )
            }
            if (darkTheme && pureBlack) base.pureBlack(true) else base
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

@Composable
fun vivimusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) = NocturneTheme(
    darkTheme = darkTheme,
    pureBlack = pureBlack,
    themeColor = themeColor,
    content = content
)

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}


