import os

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(content)

base_theme_dir = "app/src/main/java/com/meshlink/ui/designsystem/theme"
base_comp_dir = "app/src/main/java/com/meshlink/ui/designsystem/components"

# 1. MeshSpacing
write_file(f"{base_theme_dir}/MeshSpacing.kt", """package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MeshSpacing(
    val extraSmall: Dp = 2.dp,
    val small: Dp = 4.dp,
    val mediumSmall: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val mediumLarge: Dp = 16.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val extraHuge: Dp = 40.dp,
    val giant: Dp = 48.dp,
    val extraGiant: Dp = 64.dp
)

val LocalMeshSpacing = staticCompositionLocalOf { MeshSpacing() }
""")

# 2. MeshElevation
write_file(f"{base_theme_dir}/MeshElevation.kt", """package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MeshElevation(
    val none: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp
)

val LocalMeshElevation = staticCompositionLocalOf { MeshElevation() }
""")

# 3. MeshShapes
write_file(f"{base_theme_dir}/MeshShapes.kt", """package com.meshlink.ui.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class MeshShapes(
    val extraSmall: Shape = RoundedCornerShape(4.dp),
    val small: Shape = RoundedCornerShape(8.dp),
    val medium: Shape = RoundedCornerShape(12.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val extraLarge: Shape = RoundedCornerShape(24.dp),
    val pill: Shape = RoundedCornerShape(50)
)

val LocalMeshShapes = staticCompositionLocalOf { MeshShapes() }
""")

# 4. MeshAnimations
write_file(f"{base_theme_dir}/MeshAnimations.kt", """package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class MeshAnimations(
    val fast: Int = 150,
    val normal: Int = 300,
    val slow: Int = 500
)

val LocalMeshAnimations = staticCompositionLocalOf { MeshAnimations() }
""")

# 5. MeshTypography
write_file(f"{base_theme_dir}/MeshTypography.kt", """package com.meshlink.ui.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MeshTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
""")

# 6. MeshColors
write_file(f"{base_theme_dir}/MeshColors.kt", """package com.meshlink.ui.designsystem.theme

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
""")

# 7. MeshTheme
write_file(f"{base_theme_dir}/MeshTheme.kt", """package com.meshlink.ui.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MeshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (amoledDark) MeshDarkColorScheme.copy(background = BackgroundAmoled, surface = BackgroundAmoled)
            else MeshDarkColorScheme
        }
        else -> MeshLightColorScheme
    }

    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalMeshSpacing provides MeshSpacing(),
        LocalMeshElevation provides MeshElevation(),
        LocalMeshShapes provides MeshShapes(),
        LocalMeshAnimations provides MeshAnimations(),
        LocalMeshSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeshTypography,
            content = content
        )
    }
}

object MeshTheme {
    val colors: MeshSemanticColors
        @Composable
        get() = LocalMeshSemanticColors.current
    val spacing: MeshSpacing
        @Composable
        get() = LocalMeshSpacing.current
    val elevation: MeshElevation
        @Composable
        get() = LocalMeshElevation.current
    val shapes: MeshShapes
        @Composable
        get() = LocalMeshShapes.current
    val animations: MeshAnimations
        @Composable
        get() = LocalMeshAnimations.current
}
""")

