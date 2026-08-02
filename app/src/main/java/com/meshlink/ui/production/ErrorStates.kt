package com.meshlink.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * Unified Presentation-Only Error Components for Mesh-Link Phase 15.
 * Provides Error Card, Error Banner, Retry State, and Error Dialog.
 */

enum class MeshErrorType {
    NETWORK_UNAVAILABLE,
    PERMISSION_DENIED,
    CONNECTION_LOST,
    TRANSFER_FAILED,
    SYNC_FAILED,
    GENERIC
}

@Composable
fun MeshErrorCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ErrorOutline,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MeshTheme.colors.error.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        color = MeshTheme.colors.errorContainer.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MeshTheme.colors.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MeshTheme.typography.titleMedium,
                    color = MeshTheme.colors.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message,
                    style = MeshTheme.typography.bodySmall,
                    color = MeshTheme.colors.onSurfaceVariant
                )
            }

            if (onRetry != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = MeshTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MeshErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = "Retry",
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MeshTheme.colors.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MeshTheme.colors.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    style = MeshTheme.typography.bodyMedium,
                    color = MeshTheme.colors.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }

            if (actionLabel != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionLabel,
                        color = MeshTheme.colors.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MeshErrorDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "OK",
    onConfirm: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MeshTheme.colors.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MeshTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MeshTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm?.invoke()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeshTheme.colors.error,
                    contentColor = MeshTheme.colors.onError
                )
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Dismiss")
            }
        },
        modifier = modifier
    )
}
