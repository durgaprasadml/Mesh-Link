package com.meshlink.ui.designsystem.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors
import com.meshlink.ui.designsystem.theme.motion.meshPressScale

@Composable
fun MeshGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .meshPressScale()
            .clickable { onClick() }
    } else modifier

    Column(
        modifier = clickableModifier
            .meshLightGlass(shape = shape)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun MeshSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMeshSemanticColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .meshHeavyGlass(shape = shape)
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MeshTheme.typography.titleMedium,
            color = colors.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MeshTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
        } else {
            Box(modifier = Modifier.padding(bottom = 12.dp))
        }
        content()
    }
}

@Composable
fun MeshSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    val colors = LocalMeshSemanticColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.cardSurface)
            .border(1.dp, colors.border.copy(alpha = 0.5f), shape)
    ) {
        content()
    }
}

@Composable
fun MeshFloatingContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .meshFloatingGlass(shape = shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@Composable
fun MeshDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalMeshSemanticColors.current.border.copy(alpha = 0.3f),
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}
