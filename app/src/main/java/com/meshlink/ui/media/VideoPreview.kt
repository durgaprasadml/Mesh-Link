package com.meshlink.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.media.models.MediaUi
import com.meshlink.ui.media.viewer.VideoPlayer

/**
 * Fullscreen Interactive Video Preview Player component.
 */
@Composable
fun VideoPreview(
    media: MediaUi,
    onClose: () -> Unit,
    onShare: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    VideoPlayer(
        media = media,
        onClose = onClose,
        onShare = onShare,
        modifier = modifier
    )
}
