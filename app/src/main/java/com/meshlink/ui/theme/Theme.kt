package com.meshlink.ui.theme

import androidx.compose.runtime.Composable
import com.meshlink.ui.designsystem.theme.MeshTheme as CoreMeshTheme

@Composable
fun MeshTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CoreMeshTheme(
        themeMode = if (darkTheme) "DARK" else "LIGHT",
        content = content
    )
}
