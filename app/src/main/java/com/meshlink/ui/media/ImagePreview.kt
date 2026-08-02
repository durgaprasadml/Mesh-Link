package com.meshlink.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.media.models.MediaUi
import com.meshlink.ui.media.viewer.ImageViewer

/**
 * Fullscreen Interactive Zoomable Image Preview component.
 */
@Composable
fun ImagePreview(
    media: MediaUi,
    onClose: () -> Unit,
    onShare: ((MediaUi) -> Unit)? = null,
    onSave: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ImageViewer(
        media = media,
        onClose = onClose,
        onShare = onShare,
        onSave = onSave,
        modifier = modifier
    )
}
