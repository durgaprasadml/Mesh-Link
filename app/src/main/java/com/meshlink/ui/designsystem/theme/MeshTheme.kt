package com.meshlink.ui.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

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

    // Resolve Custom Primary Color
    val customPrimary = when (accentColor) {
        "Green" -> Color(0xFF4CAF50)
        "Purple" -> Color(0xFF9C27B0)
        "Orange" -> Color(0xFFFF9800)
        "Red" -> Color(0xFFF44336)
        else -> Color(0xFF2196F3) // Blue
    }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (amoledDark) MeshDarkColorScheme.copy(background = BackgroundAmoled, surface = BackgroundAmoled)
            else MeshDarkColorScheme.copy(primary = customPrimary)
        }
        else -> MeshLightColorScheme.copy(primary = customPrimary)
    }

    // Apply High Contrast modifications if needed
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

    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Adjust Density for font scaling
    val currentDensity = LocalDensity.current
    val effectiveFontScale = fontScale * (if (largeTextEnabled) 1.3f else 1.0f)
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * effectiveFontScale
    )

    // Adjust Shapes
    val shapes = MeshShapes(
        extraSmall = RoundedCornerShape(4.dp * cornerRadiusScale),
        small = RoundedCornerShape(8.dp * cornerRadiusScale),
        medium = RoundedCornerShape(12.dp * cornerRadiusScale),
        large = RoundedCornerShape(16.dp * cornerRadiusScale),
        extraLarge = RoundedCornerShape(24.dp * cornerRadiusScale)
    )
    val materialShapes = androidx.compose.material3.Shapes(
        extraSmall = RoundedCornerShape(4.dp * cornerRadiusScale),
        small = RoundedCornerShape(8.dp * cornerRadiusScale),
        medium = RoundedCornerShape(12.dp * cornerRadiusScale),
        large = RoundedCornerShape(16.dp * cornerRadiusScale),
        extraLarge = RoundedCornerShape(24.dp * cornerRadiusScale)
    )

    // Adjust Animations
    val animations = if (!animationsEnabled) {
        MeshAnimations(fast = 0, normal = 0, slow = 0)
    } else if (reduceMotionEnabled) {
        MeshAnimations(fast = 100, normal = 150, slow = 250)
    } else {
        MeshAnimations()
    }

    CompositionLocalProvider(
        LocalDensity provides customDensity,
        LocalMeshSpacing provides MeshSpacing(),
        LocalMeshElevation provides MeshElevation(),
        LocalMeshShapes provides shapes,
        LocalMeshAnimations provides animations,
        LocalMeshSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeshTypography,
            shapes = materialShapes,
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
