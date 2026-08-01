package com.meshlink.ui.designsystem.theme.icons

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

object MeshIconSizes {
    val Micro: Dp = 12.dp
    val Small: Dp = 16.dp
    val Medium: Dp = 20.dp
    val Standard: Dp = 24.dp
    val Large: Dp = 32.dp
    val Hero: Dp = 48.dp
}

object MeshIconVectors {
    val Mesh: ImageVector = Icons.Default.Hub
    val BLE: ImageVector = Icons.Default.Bluetooth
    val Wifi: ImageVector = Icons.Default.Wifi
    val Hybrid: ImageVector = Icons.Default.Router
    val Security: ImageVector = Icons.Default.Lock
    val Battery: ImageVector = Icons.Default.BatteryChargingFull
    val RSSI: ImageVector = Icons.Default.SignalCellularAlt
    val Hop: ImageVector = Icons.Default.CellTower
    val Notification: ImageVector = Icons.Default.Notifications
    val Diagnostics: ImageVector = Icons.Default.CompassCalibration
    val SOS: ImageVector = Icons.Default.Warning
    val Success: ImageVector = Icons.Default.CheckCircle
}

@Composable
fun MeshIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = MeshIconSizes.Standard,
    tint: Color = LocalMeshSemanticColors.current.textPrimary
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/** Animated icon wrapper crossfading icon vector changes smoothly. */
@Composable
fun MeshAnimatedIcon(
    targetVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = MeshIconSizes.Standard,
    tint: Color = LocalMeshSemanticColors.current.textPrimary
) {
    Crossfade(
        targetState = targetVector,
        animationSpec = tween(durationMillis = 200),
        label = "meshIconCrossfade"
    ) { vector ->
        MeshIcon(
            imageVector = vector,
            contentDescription = contentDescription,
            modifier = modifier,
            size = size,
            tint = tint
        )
    }
}
