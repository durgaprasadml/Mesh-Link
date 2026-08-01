package com.meshlink.ui.designsystem.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun MeshFAB(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    containerColor: Color = MeshTheme.colors.primary,
    contentColor: Color = Color.Black
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MeshTheme.shapes.fab,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = MeshTheme.elevation.floating
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (label != null) 20.dp else 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun FloatingDock(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.pill)
            .background(MeshTheme.colors.glassSurface)
            .border(0.5.dp, MeshTheme.colors.glassBorder, MeshTheme.shapes.pill)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun FloatingPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(MeshTheme.shapes.floating)
            .background(MeshTheme.colors.surface)
            .border(0.5.dp, MeshTheme.colors.border, MeshTheme.shapes.floating)
            .padding(16.dp)
    ) {
        content()
    }
}
