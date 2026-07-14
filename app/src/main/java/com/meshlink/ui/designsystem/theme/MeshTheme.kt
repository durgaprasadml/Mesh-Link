package com.meshlink.ui.designsystem.theme

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
