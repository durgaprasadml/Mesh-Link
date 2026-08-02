package com.meshlink.ui.chat

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Attachment picker drawer delegating to modern Material 3 [AttachmentSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onLocationClick: () -> Unit,
    onVoiceClick: () -> Unit = {},
    onDocumentClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AttachmentSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onGalleryClick = {
            onDismissRequest()
            onGalleryClick()
        },
        onCameraClick = {
            onDismissRequest()
            onCameraClick()
        },
        onFilesClick = {
            onDismissRequest()
            onDocumentClick()
        },
        onAudioClick = {
            onDismissRequest()
            onVoiceClick()
        },
        onLocationClick = {
            onDismissRequest()
            onLocationClick()
        },
        onContactClick = {
            onDismissRequest()
            onContactClick()
        },
        modifier = modifier
    )
}
