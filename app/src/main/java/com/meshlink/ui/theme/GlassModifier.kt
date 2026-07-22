package com.meshlink.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun Modifier.glassBackground(
    darkTheme: Boolean = isSystemInDarkTheme()
): Modifier {
    val backgroundColor = if (darkTheme) GlassSurfaceDark else GlassSurfaceLight
    val borderColor = if (darkTheme) GlassBorderDark else GlassBorderLight
    
    return this
        .clip(MeshTheme.shapes.large)
        .background(color = backgroundColor)
        .border(1.dp, borderColor, MeshTheme.shapes.large)
}
