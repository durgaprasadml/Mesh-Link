package com.meshlink.ui.designsystem.components.glass

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MeshTheme.shapes.glass,
    borderWidth: Dp = 0.5.dp,
    borderColor: Color = MeshTheme.colors.glassBorder,
    backgroundColor: Color = MeshTheme.colors.glassSurface,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = backgroundColor,
        shadowElevation = MeshTheme.elevation.glass
    ) {
        Box(modifier = Modifier.padding(16.dp), content = content)
    }
}
