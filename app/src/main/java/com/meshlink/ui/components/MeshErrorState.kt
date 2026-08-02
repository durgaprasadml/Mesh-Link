package com.meshlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMeDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.accessibility.meshSemantics
import com.meshlink.ui.designsystem.accessibility.meshTouchTarget48dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

enum class MeshErrorType {
    NETWORK_ERROR,
    PERMISSION_ERROR,
    DISCOVERY_ERROR,
    SYNC_ERROR,
    FILE_TRANSFER_ERROR,
    STORAGE_ERROR,
    SECURITY_ERROR,
    UNKNOWN_ERROR
}

/**
 * Standardized Error Presentation Component for Mesh-Link.
 * Displays error illustration/icon, title, description, retry button, and secondary action.
 */
@Composable
fun MeshErrorState(
    errorType: MeshErrorType = MeshErrorType.UNKNOWN_ERROR,
    title: String? = null,
    description: String? = null,
    onRetry: (() -> Unit)? = null,
    retryButtonText: String = "Try Again",
    onSecondaryAction: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    modifier: Modifier = Modifier
) {
    val (defaultIcon, defaultTitle, defaultDesc) = when (errorType) {
        MeshErrorType.NETWORK_ERROR -> Triple(
            Icons.Default.WifiOff,
            "Network Connection Failed",
            "Unable to connect to the mesh network or local radio. Please check Wi-Fi & Bluetooth settings."
        )
        MeshErrorType.PERMISSION_ERROR -> Triple(
            Icons.Default.Lock,
            "Permissions Required",
            "Mesh-Link requires Location, Nearby Devices, and Bluetooth permissions to communicate offline."
        )
        MeshErrorType.DISCOVERY_ERROR -> Triple(
            Icons.Default.NearMeDisabled,
            "Discovery Failed",
            "Could not discover nearby mesh nodes. Ensure nearby devices have Mesh-Link active."
        )
        MeshErrorType.SYNC_ERROR -> Triple(
            Icons.Default.SyncProblem,
            "Sync Interrupted",
            "Offline message sync encountered an unexpected error. Saved messages will sync automatically once resolved."
        )
        MeshErrorType.FILE_TRANSFER_ERROR -> Triple(
            Icons.Default.FolderOff,
            "File Transfer Failed",
            "The peer disconnected or rejected the payload. Please verify connection and retry."
        )
        MeshErrorType.STORAGE_ERROR -> Triple(
            Icons.Default.FolderOff,
            "Storage Space Low",
            "Insufficient local storage to save media attachments or database indexes."
        )
        MeshErrorType.SECURITY_ERROR -> Triple(
            Icons.Default.Security,
            "Security Verification Failed",
            "Could not verify identity signature or key exchange. Connection rejected for privacy protection."
        )
        MeshErrorType.UNKNOWN_ERROR -> Triple(
            Icons.Default.Warning,
            "Unexpected Error",
            "An unexpected error occurred while processing your request. Please try again."
        )
    }

    val displayTitle = title ?: defaultTitle
    val displayDesc = description ?: defaultDesc
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MeshTheme.spacing.giant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(errorColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = defaultIcon,
                contentDescription = null,
                tint = errorColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(MeshSpacing.SectionGap))

        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MeshSpacing.SM))

        Text(
            text = displayDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MeshSpacing.LG)
        )

        Spacer(modifier = Modifier.height(MeshSpacing.XXL))

        if (onRetry != null) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = MeshSpacing.XXL, vertical = MeshSpacing.MD),
                modifier = Modifier
                    .height(50.dp)
                    .meshTouchTarget48dp()
                    .meshSemantics(description = retryButtonText, role = Role.Button)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = retryButtonText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (onSecondaryAction != null && secondaryButtonText != null) {
            Spacer(modifier = Modifier.height(MeshSpacing.SM))
            OutlinedButton(
                onClick = onSecondaryAction,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = MeshSpacing.XXL, vertical = MeshSpacing.MD),
                modifier = Modifier
                    .height(48.dp)
                    .meshTouchTarget48dp()
                    .meshSemantics(description = secondaryButtonText, role = Role.Button)
            ) {
                Text(
                    text = secondaryButtonText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
