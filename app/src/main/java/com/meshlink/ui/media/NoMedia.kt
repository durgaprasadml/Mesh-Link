package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Minimalist Empty State component for Media & File Sharing.
 */
@Composable
fun NoMedia(
    title: String = "No media shared yet",
    subtitle: String = "Photos, videos and files shared through Mesh-Link will appear here.",
    onStartSharingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Folder Illustration Badge
            Surface(
                shape = CircleShape,
                color = MeshTheme.colors.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MeshTheme.colors.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = title,
                style = MeshTheme.customTypography.title.copy(fontWeight = FontWeight.Bold),
                color = MeshTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = subtitle,
                style = MeshTheme.customTypography.body,
                color = MeshTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onStartSharingClick != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartSharingClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MeshTheme.colors.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Sharing")
                }
            }
        }
    }
}
