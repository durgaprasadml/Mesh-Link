package com.meshlink.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.meshlink.ui.designsystem.theme.colors.AmoledSemanticColors
import com.meshlink.ui.designsystem.theme.colors.DarkSemanticColors
import com.meshlink.ui.designsystem.theme.colors.LightSemanticColors
import com.meshlink.ui.designsystem.theme.colors.MeshColorTokens
import com.meshlink.ui.designsystem.theme.colors.MeshSemanticColors

// ─── Primitive Tokens & Aliases ────────────────────────────────────────────────
val BrandPrimary = MeshColorTokens.CyberMint
val BrandPrimaryDark = MeshColorTokens.CyberMintDark
val BrandSecondary = MeshColorTokens.ElectricCyan

val NeuralDark = Color(0xFF060B12)
val GridSurface = Color(0xFF0E1520)
val GlowTeal = MeshColorTokens.SignalTeal
const val GlassAlpha = 0.72f

val SurfaceLight = MeshColorTokens.NeutralLightSurface
val SurfaceDark = MeshColorTokens.NeutralDarkSurface
val BackgroundLight = MeshColorTokens.NeutralLightBg
val BackgroundDark = MeshColorTokens.NeutralDarkBg
val BackgroundAmoled = MeshColorTokens.PitchBlack

val ErrorColor = MeshColorTokens.DangerRedLight
val ErrorContainerColor = Color(0xFFFEE2E2)
val ErrorColorDark = MeshColorTokens.DangerRed
val ErrorContainerColorDark = Color(0xFF4A1010)

val SuccessColor = MeshColorTokens.SuccessGreenLight
val SuccessColorDark = MeshColorTokens.SuccessGreen
val WarningColor = MeshColorTokens.WarningAmber
val InfoColor = MeshColorTokens.InfoBlue

val DangerColor = MeshColorTokens.DangerRed
val SecureColor = MeshColorTokens.SuccessGreen
val OnlineColor = MeshColorTokens.CyberMint
val OfflineColor = MeshColorTokens.MeshOffline

val SignalWeak = MeshColorTokens.SignalWeak
val SignalMedium = MeshColorTokens.SignalMedium
val SignalStrong = MeshColorTokens.SignalExcellent

// Export CompositionLocal
val LocalMeshSemanticColors = com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

// ─── Material 3 Color Schemes ─────────────────────────────────────────────────
val MeshLightColorScheme = lightColorScheme(
    primary = Color(0xFF008953),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF003820),
    secondary = Color(0xFF007A8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF00363D),
    tertiary = Color(0xFF6D5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0B3),
    onTertiaryContainer = Color(0xFF221B00),
    background = BackgroundLight,
    onBackground = MeshColorTokens.NeutralLightTextPrimary,
    surface = SurfaceLight,
    onSurface = MeshColorTokens.NeutralLightTextPrimary,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFE8EEF5),
    surfaceContainerHigh = Color(0xFFDEE5F0),
    surfaceContainerHighest = Color(0xFFD5DFED),
    outline = MeshColorTokens.NeutralLightOutline,
    outlineVariant = MeshColorTokens.NeutralLightBorder,
    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorContainerColor,
    onErrorContainer = Color(0xFF7F1D1D)
)

val MeshDarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = Color(0xFF99FFE0),
    secondary = BrandSecondary,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F59),
    onSecondaryContainer = Color(0xFFB5F5FF),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF3A2E00),
    tertiaryContainer = Color(0xFF524200),
    onTertiaryContainer = Color(0xFFFFEA9E),
    background = BackgroundDark,
    onBackground = MeshColorTokens.NeutralDarkTextPrimary,
    surface = SurfaceDark,
    onSurface = MeshColorTokens.NeutralDarkTextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF1A2232),
    surfaceContainerHigh = Color(0xFF222B3E),
    surfaceContainerHighest = Color(0xFF2B364D),
    outline = MeshColorTokens.NeutralDarkOutline,
    outlineVariant = MeshColorTokens.NeutralDarkBorder,
    error = ErrorColorDark,
    onError = Color.White,
    errorContainer = ErrorContainerColorDark,
    onErrorContainer = Color(0xFFFFD2D2)
)

val MeshAmoledColorScheme = MeshDarkColorScheme.copy(
    background = MeshColorTokens.PitchBlack,
    surface = MeshColorTokens.AmoledSurface,
    surfaceContainer = Color(0xFF0A1018),
    surfaceContainerHigh = Color(0xFF111A26),
    surfaceContainerHighest = Color(0xFF18222F)
)
