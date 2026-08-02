package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PermScanWifi
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Presentation-Only Permission Rationale Components for Mesh-Link Phase 15.
 * Explains Bluetooth, Nearby, Location, Camera, Notifications, Microphone, and Storage rationale.
 */

enum class MeshPermissionType {
    BLUETOOTH,
    NEARBY_DEVICES,
    LOCATION,
    CAMERA,
    NOTIFICATIONS,
    MICROPHONE,
    STORAGE
}

data class PermissionRationaleInfo(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val requirementReason: String
)

fun getPermissionRationale(type: MeshPermissionType): PermissionRationaleInfo {
    return when (type) {
        MeshPermissionType.BLUETOOTH -> PermissionRationaleInfo(
            title = "Bluetooth Permission",
            description = "Bluetooth LE is required to discover nearby peer devices and create direct wireless mesh links without internet access.",
            icon = Icons.Default.Bluetooth,
            requirementReason = "Used exclusively for local mesh node discovery and P2P connection handshake."
        )
        MeshPermissionType.NEARBY_DEVICES -> PermissionRationaleInfo(
            title = "Nearby Devices Permission",
            description = "Permission to scan for nearby devices allows Mesh-Link to negotiate high-speed Wi-Fi Direct and BLE mesh data connections.",
            icon = Icons.Default.PermScanWifi,
            requirementReason = "Enables high-bandwidth peer data routing."
        )
        MeshPermissionType.LOCATION -> PermissionRationaleInfo(
            title = "Location Permission",
            description = "Android requires Location permission for hardware BLE and Wi-Fi scanning. Your geographic location is never recorded or transmitted.",
            icon = Icons.Default.LocationOn,
            requirementReason = "Required by Android OS for Wi-Fi Direct and BLE beacon detection."
        )
        MeshPermissionType.CAMERA -> PermissionRationaleInfo(
            title = "Camera Permission",
            description = "Camera access allows scanning peer verification QR codes and attaching real-time tactical photos to offline broadcasts.",
            icon = Icons.Default.CameraAlt,
            requirementReason = "Used only for QR identity verification and photo attachment capture."
        )
        MeshPermissionType.NOTIFICATIONS -> PermissionRationaleInfo(
            title = "Notifications Permission",
            description = "Notifications keep you informed of incoming direct mesh messages, sync completion, and high-priority SOS alert signals.",
            icon = Icons.Default.Notifications,
            requirementReason = "Delivers timely mesh messaging alerts while running in the background."
        )
        MeshPermissionType.MICROPHONE -> PermissionRationaleInfo(
            title = "Microphone Permission",
            description = "Microphone access enables recording short push-to-talk voice notes and audio clips shared across offline mesh channels.",
            icon = Icons.Default.Mic,
            requirementReason = "Used exclusively for recording user voice messages."
        )
        MeshPermissionType.STORAGE -> PermissionRationaleInfo(
            title = "Storage Permission",
            description = "Storage access allows saving and exporting encrypted media attachments, mesh logs, and backup security keys.",
            icon = Icons.Default.Folder,
            requirementReason = "Required to save received attachments and offline security backups."
        )
    }
}

@Composable
fun MeshPermissionRationaleScreen(
    type: MeshPermissionType,
    onGrantRequested: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = getPermissionRationale(type)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = MeshTheme.colors.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = MeshTheme.colors.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = info.title,
                style = MeshTheme.typography.headlineSmall,
                color = MeshTheme.colors.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = info.description,
                style = MeshTheme.typography.bodyMedium,
                color = MeshTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = info.requirementReason,
                        style = MeshTheme.typography.bodySmall,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGrantRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeshTheme.colors.primary,
                    contentColor = MeshTheme.colors.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Grant Permission", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Not Now", color = MeshTheme.colors.onSurfaceVariant)
            }
        }
    }
}
