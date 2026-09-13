package com.meshlink.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base Brand Colors
val BrandPrimary = Color(0xFF00FF88)
val BrandPrimaryDark = Color(0xFF00CC6A)
val BrandSecondary = Color(0xFF3B82F6)

// Semantic Core Colors
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF1E1E1E)
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF121212)
val BackgroundAmoled = Color(0xFF000000)

val ErrorColor = Color(0xFFDC2626)
val ErrorContainerColor = Color(0xFFFEE2E2)
val ErrorColorDark = Color(0xFFEF4444)
val ErrorContainerColorDark = Color(0xFF7F1D1D)

val SuccessColor = Color(0xFF15803D)
val SuccessColorDark = Color(0xFF4ADE80)
val WarningColor = Color(0xFFB45309)
val WarningColorDark = Color(0xFFFBBF24)
val InfoColor = Color(0xFF0284C7)
val InfoColorDark = Color(0xFF38BDF8)

val DangerColor = Color(0xFFEF4444)
val SecureColor = Color(0xFF10B981)

val OnlineColor = Color(0xFF10B981)
val OfflineColor = Color(0xFF6B7280)

val SignalWeak = Color(0xFFEF4444)
val SignalMedium = Color(0xFFF59E0B)
val SignalStrong = Color(0xFF10B981)

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
    val amoledBackground: Color
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
    amoledBackground = BackgroundLight
)

val DarkSemanticColors = MeshSemanticColors(
    success = SuccessColorDark,
    warning = WarningColorDark,
    error = ErrorColorDark,
    info = InfoColorDark,
    danger = DangerColor,
    secure = SecureColor,
    online = OnlineColor,
    offline = OfflineColor,
    signalWeak = SignalWeak,
    signalMedium = SignalMedium,
    signalStrong = SignalStrong,
    amoledBackground = BackgroundAmoled
)

val LocalMeshSemanticColors = staticCompositionLocalOf { LightSemanticColors }

@Immutable
data class AccentColorSet(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

fun getAccentColorSet(accent: String, isDark: Boolean): AccentColorSet {
    return if (isDark) {
        when (accent) {
            "Green" -> AccentColorSet(Color(0xFF4ADE80), Color(0xFF052E16), Color(0xFF14532D), Color(0xFFDCFCE7))
            "Purple" -> AccentColorSet(Color(0xFFC084FC), Color(0xFF3B0764), Color(0xFF581C87), Color(0xFFF3E8FF))
            "Orange" -> AccentColorSet(Color(0xFFFB923C), Color(0xFF431407), Color(0xFF7C2D12), Color(0xFFFFEDD5))
            "Red" -> AccentColorSet(Color(0xFFF87171), Color(0xFF450A0A), Color(0xFF7F1D1D), Color(0xFFFEE2E2))
            else -> AccentColorSet(Color(0xFF60A5FA), Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFFDBEAFE)) // Blue
        }
    } else {
        when (accent) {
            "Green" -> AccentColorSet(Color(0xFF15803D), Color.White, Color(0xFFDCFCE7), Color(0xFF14532D))
            "Purple" -> AccentColorSet(Color(0xFF7E22CE), Color.White, Color(0xFFF3E8FF), Color(0xFF581C87))
            "Orange" -> AccentColorSet(Color(0xFFC2410C), Color.White, Color(0xFFFFEDD5), Color(0xFF7C2D12))
            "Red" -> AccentColorSet(Color(0xFFB91C1C), Color.White, Color(0xFFFEE2E2), Color(0xFF7F1D1D))
            else -> AccentColorSet(Color(0xFF1D6FDB), Color.White, Color(0xFFDBEAFE), Color(0xFF1E3A8A)) // Blue
        }
    }
}

val MeshLightColorScheme = lightColorScheme(
    primary = Color(0xFF1D6FDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF115E59),
    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorContainerColor,
    onErrorContainer = Color(0xFF7F1D1D)
)

val MeshDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E40AF),
    onSecondaryContainer = Color(0xFFEFF6FF),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = BackgroundDark,
    onBackground = Color(0xFFF8FAFC),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = ErrorColorDark,
    onError = Color.White,
    errorContainer = ErrorContainerColorDark,
    onErrorContainer = Color(0xFFFEE2E2)
)
