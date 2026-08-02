package com.meshlink.ui.media

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaFilterType

/**
 * Material 3 Filter Chips Row component for Media types: All, Images, Videos, Audio, Documents, Files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaFilters(
    selectedFilter: MediaFilterType,
    onFilterSelected: (MediaFilterType) -> Unit,
    countsMap: Map<MediaFilterType, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filterOptions = listOf(
            MediaFilterType.ALL,
            MediaFilterType.IMAGES,
            MediaFilterType.VIDEOS,
            MediaFilterType.AUDIO,
            MediaFilterType.DOCUMENTS,
            MediaFilterType.APK
        )

        filterOptions.forEach { filter ->
            val count = countsMap[filter]
            val chipLabel = if (count != null && count > 0) "${filter.label} ($count)" else filter.label
            val isSelected = filter == selectedFilter

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = chipLabel,
                        style = MeshTheme.customTypography.caption
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MeshTheme.colors.primary,
                    selectedLabelColor = MeshTheme.colors.onPrimary,
                    containerColor = MeshTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MeshTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}
