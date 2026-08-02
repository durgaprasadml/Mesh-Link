package com.meshlink.ui.media.models

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.badges.MeshChip
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Filter categories for Media and File attachments.
 */
enum class MediaFilterType(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    VOICE_NOTES("Voice Notes"),
    DOCUMENTS("Documents"),
    APK("APKs"),
    ZIP("ZIP Files"),
    PDF("PDFs"),
    FAVORITES("Favorites")
}

@Composable
fun MediaFilterChipsBar(
    selectedFilter: MediaFilterType,
    onFilterSelected: (MediaFilterType) -> Unit,
    countsMap: Map<MediaFilterType, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaFilterType.values().forEach { filter ->
            val count = countsMap[filter]
            val chipLabel = if (count != null && count > 0) "${filter.label} ($count)" else filter.label
            val isSelected = filter == selectedFilter

            MeshChip(
                label = chipLabel,
                selected = isSelected,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}
