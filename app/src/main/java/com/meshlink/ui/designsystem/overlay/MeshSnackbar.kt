package com.meshlink.ui.designsystem.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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

enum class MeshSnackbarStyle {
    Info,
    Success,
    Warning,
    Error
}

/**
 * Standardized Material 3 Snackbar component with semantic color variants.
 */
@Composable
fun MeshSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    style: MeshSnackbarStyle = MeshSnackbarStyle.Info,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val containerColor = when (style) {
        MeshSnackbarStyle.Info -> MeshTheme.colors.surfaceVariant
        MeshSnackbarStyle.Success -> MeshTheme.colors.primary.copy(alpha = 0.15f)
        MeshSnackbarStyle.Warning -> Color(0xFFFFF3E0)
        MeshSnackbarStyle.Error -> MeshTheme.colors.emergency.copy(alpha = 0.15f)
    }

    val contentColor = when (style) {
        MeshSnackbarStyle.Info -> MeshTheme.colors.textPrimary
        MeshSnackbarStyle.Success -> MeshTheme.colors.primary
        MeshSnackbarStyle.Warning -> Color(0xFFE65100)
        MeshSnackbarStyle.Error -> MeshTheme.colors.emergency
    }

    val icon: ImageVector = when (style) {
        MeshSnackbarStyle.Info -> Icons.Filled.Info
        MeshSnackbarStyle.Success -> Icons.Filled.CheckCircle
        MeshSnackbarStyle.Warning -> Icons.Filled.Warning
        MeshSnackbarStyle.Error -> Icons.Filled.Warning
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MeshTheme.shapes.medium)
            .border(0.5.dp, contentColor.copy(alpha = 0.3f), MeshTheme.shapes.medium),
        color = containerColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier
                        .clickable { onActionClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
