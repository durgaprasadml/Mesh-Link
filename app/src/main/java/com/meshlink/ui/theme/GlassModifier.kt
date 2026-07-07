package com.meshlink.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassBackground(
    darkTheme: Boolean = isSystemInDarkTheme()
): Modifier {
    val backgroundColor = if (darkTheme) GlassSurfaceDark else GlassSurfaceLight
    val borderColor = if (darkTheme) GlassBorderDark else GlassBorderLight
    
    return this
        .clip(RoundedCornerShape(16.dp))
        .background(backgroundColor)
        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
}
