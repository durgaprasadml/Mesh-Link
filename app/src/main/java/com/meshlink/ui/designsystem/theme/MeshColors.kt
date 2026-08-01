package com.meshlink.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Brand Palette ────────────────────────────────────────────────────────────
val BrandPrimary = Color(0xFF00F59B)       // Cyber Mint (dark mode primary)
val BrandPrimaryDark = Color(0xFF00C875)
val BrandSecondary = Color(0xFF00E5FF)     // Electric Cyan

// ─── "Signal" Design Language New Tokens ─────────────────────────────────────
/** Deep space-navy AMOLED background — richer than pure black. */
val NeuralDark = Color(0xFF060B12)
/** Dark glass card surface — sits above NeuralDark. */
val GridSurface = Color(0xFF0E1520)
/** Shifted mint for glow/bloom effects in dark mode. */
val GlowTeal = Color(0xFF00F5A8)
/** Glass surface alpha — shared across glass card implementations. */
const val GlassAlpha = 0.72f

// ─── Surface & Background Tokens ─────────────────────────────────────────────
val SurfaceLight = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF141A26)
val BackgroundLight = Color(0xFFFAFAFC)
val BackgroundDark = Color(0xFF0B0F17)
val BackgroundAmoled = NeuralDark

// ─── Semantic State Colors ────────────────────────────────────────────────────
val ErrorColor = Color(0xFFDC2626)
val ErrorContainerColor = Color(0xFFFEE2E2)
val ErrorColorDark = Color(0xFFFF453A)
val ErrorContainerColorDark = Color(0xFF4A1010)

val SuccessColor = Color(0xFF16A34A)
val SuccessColorDark = Color(0xFF30D158)
val WarningColor = Color(0xFFFF9F0A)
val InfoColor = Color(0xFF64D2FF)

val DangerColor = Color(0xFFFF453A)
val SecureColor = Color(0xFF30D158)

val OnlineColor = Color(0xFF00F59B)
val OfflineColor = Color(0xFF8E8E93)

val SignalWeak = Color(0xFFFF453A)
val SignalMedium = Color(0xFFFF9F0A)
val SignalStrong = Color(0xFF00F59B)

// ─── Semantic Color Set ───────────────────────────────────────────────────────
@Immutable
data class MeshSemanticColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val danger: Color,
    val secure: Color,
    val online: Color,
    val offline: Color,
    val signalWeak: Color,
    val signalMedium: Color,
    val signalStrong: Color,
    val amoledBackground: Color,
    /** Glow/bloom teal for dark mode ambient effects. */
    val glowAccent: Color
)

val LightSemanticColors = MeshSemanticColors(
    success = SuccessColor,
    warning = WarningColor,
    error = ErrorColor,
    info = InfoColor,
    danger = DangerColor,
    secure = SecureColor,
    online = OnlineColor,
    offline = OfflineColor,
    signalWeak = SignalWeak,
    signalMedium = SignalMedium,
    signalStrong = SignalStrong,
    amoledBackground = BackgroundLight,
    glowAccent = BrandPrimary
)

val DarkSemanticColors = MeshSemanticColors(
    success = SuccessColorDark,
    warning = WarningColor,
    error = ErrorColorDark,
    info = InfoColor,
    danger = DangerColor,
    secure = SecureColor,
    online = OnlineColor,
    offline = OfflineColor,
    signalWeak = SignalWeak,
    signalMedium = SignalMedium,
    signalStrong = SignalStrong,
    amoledBackground = BackgroundAmoled,
    glowAccent = GlowTeal
)

val LocalMeshSemanticColors = staticCompositionLocalOf { LightSemanticColors }

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
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFE8EEF5),
    surfaceContainerHigh = Color(0xFFDEE5F0),
    surfaceContainerHighest = Color(0xFFD5DFED),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
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
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF1A2232),
    surfaceContainerHigh = Color(0xFF222B3E),
    surfaceContainerHighest = Color(0xFF2B364D),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF2D3A4F),
    error = ErrorColorDark,
    onError = Color.White,
    errorContainer = ErrorContainerColorDark,
    onErrorContainer = Color(0xFFFFD2D2)
)

// ─── AMOLED Color Scheme ─────────────────────────────────────────────────────
// Derives from dark scheme but with NeuralDark backgrounds and GridSurface surfaces
val MeshAmoledColorScheme = MeshDarkColorScheme.copy(
    background = NeuralDark,
    surface = GridSurface,
    surfaceContainer = Color(0xFF0A1018),
    surfaceContainerHigh = Color(0xFF111A26),
    surfaceContainerHighest = Color(0xFF18222F)
)
