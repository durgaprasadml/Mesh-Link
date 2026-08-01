package com.meshlink.ui.designsystem.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

@Composable
fun MeshTag(
    text: String,
    modifier: Modifier = Modifier
) {
    MeshBadge(text = text, modifier = modifier)
}

@Composable
fun MeshPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = LocalMeshSemanticColors.current.elevatedSurface,
    contentColor: Color = LocalMeshSemanticColors.current.textPrimary
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, LocalMeshSemanticColors.current.border.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, style = MeshTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
fun MeshStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    pulse: Boolean = false
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MeshNotificationBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    val text = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(LocalMeshSemanticColors.current.danger)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MeshTheme.typography.labelSmall, color = Color.White)
    }
}
