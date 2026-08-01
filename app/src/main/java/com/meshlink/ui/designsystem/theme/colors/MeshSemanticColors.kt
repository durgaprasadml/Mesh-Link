package com.meshlink.ui.designsystem.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic Color Data Structure for Mesh Link 2026 Design System.
 * Supports Light, Dark, and AMOLED themes.
 */
@Immutable
data class MeshSemanticColors(
    val primary: Color,
    val secondary: Color,
    val primaryBackground: Color,
    val secondaryBackground: Color,
    val elevatedSurface: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val cardSurface: Color,
    val meshConnected: Color,
    val meshSearching: Color,
    val meshOffline: Color,
    val warning: Color,
    val danger: Color,
    val success: Color,
    val info: Color,
    val signalExcellent: Color,
    val signalMedium: Color,
    val signalWeak: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val outline: Color,
    val border: Color,
    val ripple: Color,
    val focus: Color,
    val pressed: Color,
    val disabled: Color,
    val glowAccent: Color,
    // Backward compatibility aliases for existing UI components
    val signalStrong: Color = signalExcellent,
    val secure: Color = success,
    val online: Color = meshConnected,
    val offline: Color = meshOffline,
    val error: Color = danger,
    val amoledBackground: Color = primaryBackground
)

val LightSemanticColors = MeshSemanticColors(
    primary = Color(0xFF008953),
    secondary = Color(0xFF007A8C),
    primaryBackground = MeshColorTokens.NeutralLightBg,
    secondaryBackground = Color(0xFFEDF2F7),
    elevatedSurface = MeshColorTokens.NeutralLightElevated,
    glassSurface = MeshColorTokens.GlassSurfaceLight,
    glassBorder = MeshColorTokens.GlassBorderLight,
    cardSurface = MeshColorTokens.NeutralLightSurface,
    meshConnected = MeshColorTokens.SuccessGreenLight,
    meshSearching = MeshColorTokens.WarningAmber,
    meshOffline = MeshColorTokens.MeshOffline,
    warning = MeshColorTokens.WarningAmber,
    danger = MeshColorTokens.DangerRedLight,
    success = MeshColorTokens.SuccessGreenLight,
    info = Color(0xFF0284C7),
    signalExcellent = MeshColorTokens.SuccessGreenLight,
    signalMedium = MeshColorTokens.WarningAmber,
    signalWeak = MeshColorTokens.DangerRedLight,
    textPrimary = MeshColorTokens.NeutralLightTextPrimary,
    textSecondary = MeshColorTokens.NeutralLightTextSecondary,
    textTertiary = MeshColorTokens.NeutralLightTextTertiary,
    outline = MeshColorTokens.NeutralLightOutline,
    border = MeshColorTokens.NeutralLightBorder,
    ripple = Color(0x1F000000),
    focus = Color(0xFF008953),
    pressed = Color(0x1A000000),
    disabled = Color(0x6694A3B8),
    glowAccent = Color(0xFF008953)
)

val DarkSemanticColors = MeshSemanticColors(
    primary = MeshColorTokens.CyberMint,
    secondary = MeshColorTokens.ElectricCyan,
    primaryBackground = MeshColorTokens.NeutralDarkBg,
    secondaryBackground = Color(0xFF0F1522),
    elevatedSurface = MeshColorTokens.NeutralDarkElevated,
    glassSurface = MeshColorTokens.GlassSurfaceDark,
    glassBorder = MeshColorTokens.GlassBorderDark,
    cardSurface = MeshColorTokens.NeutralDarkSurface,
    meshConnected = MeshColorTokens.MeshConnected,
    meshSearching = MeshColorTokens.MeshSearching,
    meshOffline = MeshColorTokens.MeshOffline,
    warning = MeshColorTokens.WarningAmber,
    danger = MeshColorTokens.DangerRed,
    success = MeshColorTokens.SuccessGreen,
    info = MeshColorTokens.InfoBlue,
    signalExcellent = MeshColorTokens.SignalExcellent,
    signalMedium = MeshColorTokens.SignalMedium,
    signalWeak = MeshColorTokens.SignalWeak,
    textPrimary = MeshColorTokens.NeutralDarkTextPrimary,
    textSecondary = MeshColorTokens.NeutralDarkTextSecondary,
    textTertiary = MeshColorTokens.NeutralDarkTextTertiary,
    outline = MeshColorTokens.NeutralDarkOutline,
    border = MeshColorTokens.NeutralDarkBorder,
    ripple = Color(0x33FFFFFF),
    focus = MeshColorTokens.CyberMint,
    pressed = Color(0x33FFFFFF),
    disabled = Color(0x66475569),
    glowAccent = MeshColorTokens.SignalTeal
)

val AmoledSemanticColors = MeshSemanticColors(
    primary = MeshColorTokens.CyberMint,
    secondary = MeshColorTokens.ElectricCyan,
    primaryBackground = MeshColorTokens.PitchBlack,
    secondaryBackground = Color(0xFF05080C),
    elevatedSurface = MeshColorTokens.AmoledElevated,
    glassSurface = MeshColorTokens.GlassSurfaceAmoled,
    glassBorder = MeshColorTokens.GlassBorderAmoled,
    cardSurface = MeshColorTokens.AmoledSurface,
    meshConnected = MeshColorTokens.MeshConnected,
    meshSearching = MeshColorTokens.MeshSearching,
    meshOffline = MeshColorTokens.MeshOffline,
    warning = MeshColorTokens.WarningAmber,
    danger = MeshColorTokens.DangerRed,
    success = MeshColorTokens.SuccessGreen,
    info = MeshColorTokens.InfoBlue,
    signalExcellent = MeshColorTokens.SignalExcellent,
    signalMedium = MeshColorTokens.SignalMedium,
    signalWeak = MeshColorTokens.SignalWeak,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    outline = MeshColorTokens.AmoledOutline,
    border = MeshColorTokens.AmoledBorder,
    ripple = Color(0x4000F59B),
    focus = MeshColorTokens.CyberMint,
    pressed = Color(0x4000F59B),
    disabled = Color(0x66334155),
    glowAccent = MeshColorTokens.CyberMint
)

val LocalMeshSemanticColors = staticCompositionLocalOf { LightSemanticColors }
