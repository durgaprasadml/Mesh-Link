package com.meshlink.ui.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meshlink.domain.model.Message
import java.io.File

/**
 * Material 3 Image Message Bubble composable.
 * Supports image file, base64 data, upload/download progress, retry, caption, and fullscreen tap callback.
 */
@Composable
fun ImageMessage(
    message: Message,
    transferProgress: Float? = null,
    onImageClick: (String) -> Unit = {},
    onRetryMedia: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val localFile = remember(message.mediaPath) {
        message.mediaPath?.let { pathStr ->
            if (pathStr.startsWith("/")) File(pathStr) else null
        }
    }

    val base64Bitmap = remember(message.thumbnailBase64) {
        message.thumbnailBase64?.let { b64 ->
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        modifier = modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onImageClick(message.messageId) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            when {
                localFile != null && localFile.exists() -> {
                    AsyncImage(
                        model = localFile,
                        contentDescription = "Attachment Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
                base64Bitmap != null -> {
                    Image(
                        bitmap = base64Bitmap,
                        contentDescription = "Thumbnail Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Image unavailable",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Transfer progress or retry overlay
            if (transferProgress != null && transferProgress < 1.0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { transferProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (localFile != null && !localFile.exists() && base64Bitmap == null) {
                IconButton(
                    onClick = { onRetryMedia(message.messageId) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry image transfer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (message.text.isNotBlank()) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
