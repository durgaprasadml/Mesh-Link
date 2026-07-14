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

val SuccessColor = Color(0xFF16A34A)
val SuccessColorDark = Color(0xFF4ADE80)
val WarningColor = Color(0xFFF59E0B)
val InfoColor = Color(0xFF06B6D4)

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
    amoledBackground = BackgroundAmoled
)

val LocalMeshSemanticColors = staticCompositionLocalOf { LightSemanticColors }

val MeshLightColorScheme = lightColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorContainerColor,
    onErrorContainer = Color(0xFF7F1D1D)
)

val MeshDarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E5),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = ErrorColorDark,
    onError = Color.White,
    errorContainer = ErrorContainerColorDark,
    onErrorContainer = Color(0xFFFEE2E2)
)
