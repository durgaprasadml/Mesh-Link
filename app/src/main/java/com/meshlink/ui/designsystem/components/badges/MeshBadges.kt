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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.meshlink.ui.designsystem.theme.icons.MeshIconVectors
import com.meshlink.ui.designsystem.theme.motion.meshPulseEffect

@Composable
fun MeshBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalMeshSemanticColors.current.primary.copy(alpha = 0.15f),
    contentColor: Color = LocalMeshSemanticColors.current.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MeshTheme.typography.labelSmall, color = contentColor)
    }
}

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
    pulse: Boolean = true
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .meshPulseEffect(enabled = pulse)
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

@Composable
fun MeshHopBadge(
    hopCount: Int,
    modifier: Modifier = Modifier
) {
    val text = when (hopCount) {
        0 -> "Direct"
        1 -> "1 Hop"
        else -> "$hopCount Hops"
    }
    val color = if (hopCount == 0) LocalMeshSemanticColors.current.meshConnected else LocalMeshSemanticColors.current.info
    MeshPill(text = text, icon = MeshIconVectors.Hop, contentColor = color, modifier = modifier)
}

@Composable
fun MeshTransportBadge(
    transportName: String,
    modifier: Modifier = Modifier
) {
    val icon = when (transportName.uppercase()) {
        "BLE" -> MeshIconVectors.BLE
        "WIFI", "WI-FI DIRECT" -> MeshIconVectors.Wifi
        else -> MeshIconVectors.Hybrid
    }
    MeshPill(text = transportName, icon = icon, modifier = modifier)
}

@Composable
fun MeshSecurityBadge(
    securityLevel: String = "AES-256-GCM",
    modifier: Modifier = Modifier
) {
    MeshPill(
        text = securityLevel,
        icon = MeshIconVectors.Security,
        contentColor = LocalMeshSemanticColors.current.meshConnected,
        modifier = modifier
    )
}

@Composable
fun MeshDeviceBadge(
    deviceName: String,
    modifier: Modifier = Modifier
) {
    MeshPill(text = deviceName, modifier = modifier)
}
