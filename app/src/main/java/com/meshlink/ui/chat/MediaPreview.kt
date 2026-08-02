package com.meshlink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meshlink.domain.model.Message
import java.io.File

/**
 * Fullscreen Media Preview dialog component for viewing image/video/file attachments.
 */
@Composable
fun MediaPreview(
    message: Message,
    onBack: () -> Unit,
    onDelete: (Message) -> Unit = {},
    onShare: (Message) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Media Content
            if (message.mediaPath != null) {
                AsyncImage(
                    model = File(message.mediaPath),
                    contentDescription = "Fullscreen preview",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Action Toolbar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { onShare(message) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share media",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { onDelete(message) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete media",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
