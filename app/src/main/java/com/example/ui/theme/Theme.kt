package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val LightMaterialScheme = lightColorScheme(
    primary = ObsidianInk,
    onPrimary = SurfaceWhite,
    primaryContainer = LavenderFillLight,
    onPrimaryContainer = LavenderInkLight,
    secondary = CoolSlate,
    onSecondary = SurfaceWhite,
    secondaryContainer = SoftMistLight,
    onSecondaryContainer = ObsidianInk,
    tertiary = MintInkLight,
    tertiaryContainer = MintFillLight,
    onTertiaryContainer = MintInkLight,
    error = CoralInkLight,
    errorContainer = CoralFillLight,
    onErrorContainer = CoralInkLight,
    background = CanvasLight,
    onBackground = ObsidianInk,
    surface = SurfaceWhite,
    onSurface = ObsidianInk,
    surfaceVariant = SoftMistLight,
    onSurfaceVariant = CoolSlate,
    outline = ColorTokens.BorderLight,
)

private val DarkMaterialScheme = darkColorScheme(
    primary = InkLight,
    onPrimary = CanvasDark,
    primaryContainer = LavenderFillDark,
    onPrimaryContainer = LavenderInkDark,
    secondary = SlateDark,
    onSecondary = CanvasDark,
    secondaryContainer = SoftMistDark,
    onSecondaryContainer = InkLight,
    tertiary = MintInkDark,
    tertiaryContainer = MintFillDark,
    onTertiaryContainer = MintInkDark,
    error = CoralInkDark,
    errorContainer = CoralFillDark,
    onErrorContainer = CoralInkDark,
    background = CanvasDark,
    onBackground = InkLight,
    surface = SurfaceDark,
    onSurface = InkLight,
    surfaceVariant = SoftMistDark,
    onSurfaceVariant = SlateDark,
    outline = ColorTokens.BorderDark,
)

private object ColorTokens {
    val BorderLight = androidx.compose.ui.graphics.Color(0xFFE9EBEF)
    val BorderDark = androidx.compose.ui.graphics.Color(0xFF272A32)
}

val SliceSplitShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun SliceSplitTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val sliceColors = if (isDark) DarkSliceSplitColors else LightSliceSplitColors
    val materialScheme = if (isDark) DarkMaterialScheme else LightMaterialScheme

    CompositionLocalProvider(LocalSliceSplitColors provides sliceColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = SliceSplitTypography,
            shapes = SliceSplitShapes,
            content = content,
        )
    }
}

object SliceTheme {
    val colors: SliceSplitColors
        @Composable
        get() = LocalSliceSplitColors.current
}
