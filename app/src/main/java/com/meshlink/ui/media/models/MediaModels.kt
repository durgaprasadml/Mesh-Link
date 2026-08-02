package com.meshlink.ui.media.models

import androidx.compose.runtime.Immutable

/**
 * UI-only presentation models for Media and Rich Content.
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE_NOTE,
    DOCUMENT,
    APK,
    ZIP,
    PDF,
    LOCATION,
    CONTACT
}

enum class MediaSortOrder {
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    NAME_ASC
}

@Immutable
data class MediaUi(
    val id: String,
    val title: String,
    val uriOrPath: String,
    val mimeType: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val timestampMs: Long,
    val senderName: String,
    val senderId: String = "",
    val thumbnailBase64: String? = null,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val isFavorite: Boolean = false,
    val isFromMe: Boolean = false,
    val transferStatusName: String = "COMPLETED"
)

@Immutable
data class PreviewState(
    val isVisible: Boolean = false,
    val selectedItem: MediaUi? = null,
    val caption: String = "",
    val isCompressed: Boolean = true,
    val recipientName: String = "Mesh Network"
)
