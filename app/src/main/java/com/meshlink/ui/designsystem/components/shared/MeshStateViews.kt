package com.meshlink.ui.designsystem.components.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.buttons.MeshButton
import com.meshlink.ui.designsystem.components.glass.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

@Composable
fun MeshEmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
    actionTitle: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalMeshSemanticColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MeshTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MeshTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionTitle != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            MeshButton(text = actionTitle, onClick = onActionClick)
        }
    }
}

@Composable
fun MeshLoadingState(
    modifier: Modifier = Modifier,
    label: String = "Loading..."
) {
    val colors = LocalMeshSemanticColors.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(44.dp), color = MeshTheme.colors.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = label, style = MeshTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
fun MeshErrorState(
    title: String = "Something went wrong",
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MeshEmptyStateView(
        title = title,
        message = message,
        icon = Icons.Default.ErrorOutline,
        actionTitle = "Retry",
        onActionClick = onRetryClick,
        modifier = modifier
    )
}

@Composable
fun MeshOfflineState(
    modifier: Modifier = Modifier,
    onSearchMesh: (() -> Unit)? = null
) {
    MeshGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = LocalMeshSemanticColors.current.meshOffline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mesh Network Offline",
                style = MeshTheme.typography.titleMedium,
                color = LocalMeshSemanticColors.current.textPrimary
            )
            Text(
                text = "No active peer connections found nearby.",
                style = MeshTheme.typography.bodySmall,
                color = LocalMeshSemanticColors.current.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            if (onSearchMesh != null) {
                MeshButton(text = "Scan Nearby Nodes", onClick = onSearchMesh)
            }
        }
    }
}
