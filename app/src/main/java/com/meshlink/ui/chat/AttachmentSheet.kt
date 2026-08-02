package com.meshlink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AttachmentGridOption(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Material 3 Modal Bottom Sheet Grid for selecting attachments (Gallery, Camera, Files, Audio, Location, Contact).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onFilesClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val options = listOf(
        AttachmentGridOption("gallery", "Gallery", Icons.Default.PhotoLibrary, MaterialTheme.colorScheme.primary),
        AttachmentGridOption("camera", "Camera", Icons.Default.CameraAlt, MaterialTheme.colorScheme.tertiary),
        AttachmentGridOption("files", "Files", Icons.Default.InsertDriveFile, MaterialTheme.colorScheme.secondary),
        AttachmentGridOption("audio", "Audio", Icons.Default.AudioFile, MaterialTheme.colorScheme.error),
        AttachmentGridOption("location", "Location", Icons.Default.LocationOn, MaterialTheme.colorScheme.primaryContainer),
        AttachmentGridOption("contact", "Contact", Icons.Default.ContactPage, MaterialTheme.colorScheme.surfaceTint)
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 8.dp)
        ) {
            Text(
                text = "Share Content",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options, key = { it.id }) { option ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                when (option.id) {
                                    "gallery" -> onGalleryClick()
                                    "camera" -> onCameraClick()
                                    "files" -> onFilesClick()
                                    "audio" -> onAudioClick()
                                    "location" -> onLocationClick()
                                    "contact" -> onContactClick()
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(option.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.title,
                                tint = option.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
