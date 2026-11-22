package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Brand & Canvas Tokens
val CanvasLight = Color(0xFFFAFAFB)
val SurfaceWhite = Color(0xFFFFFFFF)
val ObsidianInk = Color(0xFF111214)
val CoolSlate = Color(0xFF6B7280)
val SoftMistLight = Color(0xFFF2F3F5)

// Dark Mode Tokens
val CanvasDark = Color(0xFF0D0E10)
val SurfaceDark = Color(0xFF17191D)
val InkLight = Color(0xFFF5F6F8)
val SlateDark = Color(0xFF9CA3AF)
val SoftMistDark = Color(0xFF22252B)

// Semantic Pastel Accents (Light)
val MintFillLight = Color(0xFFDCFCE7)
val MintInkLight = Color(0xFF14532D)
val CoralFillLight = Color(0xFFFFE4E6)
val CoralInkLight = Color(0xFF881337)
val LavenderFillLight = Color(0xFFEDE9FE)
val LavenderInkLight = Color(0xFF4C1D95)
val SkyFillLight = Color(0xFFE0F2FE)
val SkyInkLight = Color(0xFF0C4A6E)
val AmberFillLight = Color(0xFFFEF3C7)
val AmberInkLight = Color(0xFF78350F)

// Semantic Pastel Accents (Dark)
val MintFillDark = Color(0xFF133120)
val MintInkDark = Color(0xFF86EFAC)
val CoralFillDark = Color(0xFF3A1820)
val CoralInkDark = Color(0xFFFDA4AF)
val LavenderFillDark = Color(0xFF261E45)
val LavenderInkDark = Color(0xFFC4B5FD)
val SkyFillDark = Color(0xFF132C3C)
val SkyInkDark = Color(0xFF7DD3FC)
val AmberFillDark = Color(0xFF342712)
val AmberInkDark = Color(0xFFFCD34D)

@Immutable
data class SliceSplitColors(
    val canvas: Color,
    val card: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val mist: Color,
    val divider: Color,
    val dockBackground: Color,
    val dockContent: Color,
    val dockInactive: Color,
    val mintFill: Color,
    val mintInk: Color,
    val coralFill: Color,
    val coralInk: Color,
    val lavenderFill: Color,
    val lavenderInk: Color,
    val skyFill: Color,
    val skyInk: Color,
    val amberFill: Color,
    val amberInk: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val isDark: Boolean,
)

val LightSliceSplitColors = SliceSplitColors(
    canvas = CanvasLight,
    card = SurfaceWhite,
    inkPrimary = ObsidianInk,
    inkSecondary = CoolSlate,
    mist = SoftMistLight,
    divider = Color(0xFFE9EBEF),
    dockBackground = ObsidianInk,
    dockContent = SurfaceWhite,
    dockInactive = Color(0xFF9CA3AF),
    mintFill = MintFillLight,
    mintInk = MintInkLight,
    coralFill = CoralFillLight,
    coralInk = CoralInkLight,
    lavenderFill = LavenderFillLight,
    lavenderInk = LavenderInkLight,
    skyFill = SkyFillLight,
    skyInk = SkyInkLight,
    amberFill = AmberFillLight,
    amberInk = AmberInkLight,
    shimmerBase = Color(0xFFECEEF2),
    shimmerHighlight = Color(0xFFF7F8FA),
    isDark = false,
)

val DarkSliceSplitColors = SliceSplitColors(
    canvas = CanvasDark,
    card = SurfaceDark,
    inkPrimary = InkLight,
    inkSecondary = SlateDark,
    mist = SoftMistDark,
    divider = Color(0xFF272A32),
    dockBackground = Color(0xFF1E2026),
    dockContent = InkLight,
    dockInactive = Color(0xFF6B7280),
    mintFill = MintFillDark,
    mintInk = MintInkDark,
    coralFill = CoralFillDark,
    coralInk = CoralInkDark,
    lavenderFill = LavenderFillDark,
    lavenderInk = LavenderInkDark,
    skyFill = SkyFillDark,
    skyInk = SkyInkDark,
    amberFill = AmberFillDark,
    amberInk = AmberInkDark,
    shimmerBase = Color(0xFF1E2026),
    shimmerHighlight = Color(0xFF2A2D35),
    isDark = true,
)

val LocalSliceSplitColors = staticCompositionLocalOf { LightSliceSplitColors }
