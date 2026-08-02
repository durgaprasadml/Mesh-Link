package com.meshlink.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Modern Material 3 Bottom Sheet for choosing or capturing profile photos and avatars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPicker(
    sheetState: SheetState,
    hasCustomImage: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onChooseAvatar: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MeshTheme.spacing.large)
        ) {
            Text(
                text = "Choose Profile Picture",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = MeshTheme.spacing.large, vertical = MeshTheme.spacing.medium)
            )

            AvatarOptionRow(
                icon = Icons.Default.CameraAlt,
                title = "Take Photo",
                description = "Capture a new picture using device camera",
                onClick = {
                    onDismiss()
                    onTakePhoto()
                }
            )

            AvatarOptionRow(
                icon = Icons.Default.PhotoLibrary,
                title = "Choose from Gallery",
                description = "Select an existing photo from device storage",
                onClick = {
                    onDismiss()
                    onChooseGallery()
                }
            )

            AvatarOptionRow(
                icon = Icons.Default.Face,
                title = "Choose Illustrated Avatar",
                description = "Pick from curated preset character avatars",
                onClick = {
                    onDismiss()
                    onChooseAvatar()
                }
            )

            if (hasCustomImage) {
                AvatarOptionRow(
                    icon = Icons.Default.Delete,
                    title = "Remove Photo",
                    description = "Reset to default initial avatar",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        onRemovePhoto()
                    }
                )
            }

            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))
        }
    }
}

@Composable
private fun AvatarOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MeshTheme.spacing.large, vertical = MeshTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.padding(end = MeshTheme.spacing.medium)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
