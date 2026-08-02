package com.meshlink.ui.media.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.document.DocumentCard
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi

/**
 * Universal fullscreen viewer container routing automatically based on [MediaType].
 */
@Composable
fun MediaViewer(
    media: MediaUi?,
    onClose: () -> Unit,
    onShare: ((MediaUi) -> Unit)? = null,
    onSave: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (media == null) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (media.mediaType) {
                MediaType.IMAGE -> {
                    ImageViewer(
                        media = media,
                        onClose = onClose,
                        onShare = onShare,
                        onSave = onSave
                    )
                }

                MediaType.VIDEO -> {
                    VideoPlayer(
                        media = media,
                        onClose = onClose,
                        onShare = onShare
                    )
                }

                MediaType.AUDIO -> {
                    AudioPlayer(
                        media = media,
                        onClose = onClose,
                        onShare = onShare
                    )
                }

                MediaType.VOICE_NOTE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VoiceMessagePlayer(
                            media = media
                        )
                    }
                }

                else -> {
                    // Document, APK, ZIP, PDF, Location, Contact
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DocumentCard(
                                media = media,
                                onOpenClick = { /* Open doc */ },
                                onShareClick = onShare,
                                onDownloadClick = onSave
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Viewer",
                                    tint = MeshTheme.colors.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
