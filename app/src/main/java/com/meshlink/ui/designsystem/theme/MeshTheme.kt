package com.meshlink.ui.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.meshlink.ui.designsystem.theme.accessibility.LocalMeshAccessibilityRules
import com.meshlink.ui.designsystem.theme.accessibility.MeshAccessibilityRules
import com.meshlink.ui.designsystem.theme.colors.AmoledSemanticColors
import com.meshlink.ui.designsystem.theme.colors.DarkSemanticColors
import com.meshlink.ui.designsystem.theme.colors.LightSemanticColors
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors
import com.meshlink.ui.designsystem.theme.colors.MeshSemanticColors
import com.meshlink.ui.designsystem.theme.elevation.LocalMeshElevation
import com.meshlink.ui.designsystem.theme.elevation.MeshElevationScale
import com.meshlink.ui.designsystem.theme.motion.LocalMeshMotion
import com.meshlink.ui.designsystem.theme.motion.MeshMotion
import com.meshlink.ui.designsystem.theme.responsive.LocalMeshWindowSize
import com.meshlink.ui.designsystem.theme.responsive.MeshWindowSize
import com.meshlink.ui.designsystem.theme.responsive.rememberMeshWindowSize
import com.meshlink.ui.designsystem.theme.shapes.LocalMeshShapes
import com.meshlink.ui.designsystem.theme.shapes.MeshShapeScale
import com.meshlink.ui.designsystem.theme.spacing.LocalMeshSpacing
import com.meshlink.ui.designsystem.theme.spacing.MeshSpacingScale
import com.meshlink.ui.designsystem.theme.typography.LocalMeshTypography
import com.meshlink.ui.designsystem.theme.typography.MeshTypographyScale
import com.meshlink.ui.designsystem.theme.typography.toMaterial3Typography

val LocalGlassEffects = staticCompositionLocalOf { true }
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun MeshTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    accentColor: String = "Blue",
    fontScale: Float = 1.0f,
    largeTextEnabled: Boolean = false,
    cornerRadiusScale: Float = 1.0f,
    animationsEnabled: Boolean = true,
    glassEffectsEnabled: Boolean = true,
    highContrast: Boolean = false,
    reduceMotionEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val customPrimary = when (accentColor) {
        "Green" -> Color(0xFF4CAF50)
        "Purple" -> Color(0xFF9C27B0)
        "Orange" -> Color(0xFFFF9800)
        "Red" -> Color(0xFFF44336)
        else -> Color(0xFF00F59B)
    }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (amoledDark) MeshAmoledColorScheme.copy(primary = customPrimary)
            else MeshDarkColorScheme.copy(primary = customPrimary)
        }
        else -> MeshLightColorScheme.copy(primary = customPrimary)
    }

    val colorScheme = if (highContrast) {
        if (darkTheme) baseColorScheme.copy(
            surface = Color.Black,
            background = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White
        ) else baseColorScheme.copy(
            surface = Color.White,
            background = Color.White,
            onSurface = Color.Black,
            onBackground = Color.Black
        )
    } else baseColorScheme

    val semanticColors = when {
        darkTheme && amoledDark -> AmoledSemanticColors
        darkTheme -> DarkSemanticColors
        else -> LightSemanticColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val currentDensity = LocalDensity.current
    val effectiveFontScale = fontScale * (if (largeTextEnabled) 1.3f else 1.0f)
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * effectiveFontScale
    )

    val shapes = MeshShapeScale(
        tiny = RoundedCornerShape(4.dp * cornerRadiusScale),
        small = RoundedCornerShape(8.dp * cornerRadiusScale),
        medium = RoundedCornerShape(12.dp * cornerRadiusScale),
        large = RoundedCornerShape(16.dp * cornerRadiusScale),
        xl = RoundedCornerShape(24.dp * cornerRadiusScale),
        jumbo = RoundedCornerShape(32.dp * cornerRadiusScale)
    )
    val materialShapes = androidx.compose.material3.Shapes(
        extraSmall = RoundedCornerShape(4.dp * cornerRadiusScale),
        small = RoundedCornerShape(8.dp * cornerRadiusScale),
        medium = RoundedCornerShape(12.dp * cornerRadiusScale),
        large = RoundedCornerShape(16.dp * cornerRadiusScale),
        extraLarge = RoundedCornerShape(24.dp * cornerRadiusScale)
    )

    val windowSize = rememberMeshWindowSize()
    val accessibilityRules = MeshAccessibilityRules(
        highContrastEnabled = highContrast,
        reduceMotionEnabled = reduceMotionEnabled
    )

    val typographyScale = MeshTypographyScale()

    CompositionLocalProvider(
        LocalDensity provides customDensity,
        LocalMeshSpacing provides MeshSpacingScale(),
        LocalMeshElevation provides MeshElevationScale(),
        LocalMeshShapes provides shapes,
        LocalMeshMotion provides MeshMotion,
        LocalMeshSemanticColors provides semanticColors,
        LocalMeshWindowSize provides windowSize,
        LocalMeshAccessibilityRules provides accessibilityRules,
        LocalMeshTypography provides typographyScale,
        LocalGlassEffects provides glassEffectsEnabled,
        LocalReduceMotion provides reduceMotionEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyScale.toMaterial3Typography(),
            shapes = materialShapes,
            content = content
        )
    }
}

object MeshTheme {
    val colors: MeshSemanticColors
        @Composable
        get() = LocalMeshSemanticColors.current
    val typography: Typography
        @Composable
        get() = MaterialTheme.typography
    val customTypography: MeshTypographyScale
        @Composable
        get() = LocalMeshTypography.current
    val spacing: MeshSpacingScale
        @Composable
        get() = LocalMeshSpacing.current
    val elevation: MeshElevationScale
        @Composable
        get() = LocalMeshElevation.current
    val shapes: MeshShapeScale
        @Composable
        get() = LocalMeshShapes.current
    val motion: MeshMotion
        @Composable
        get() = LocalMeshMotion.current
    val windowSize: MeshWindowSize
        @Composable
        get() = LocalMeshWindowSize.current
    val accessibility: MeshAccessibilityRules
        @Composable
        get() = LocalMeshAccessibilityRules.current
    val glassEffects: Boolean
        @Composable
        get() = LocalGlassEffects.current
    val reduceMotion: Boolean
        @Composable
        get() = LocalReduceMotion.current
}
