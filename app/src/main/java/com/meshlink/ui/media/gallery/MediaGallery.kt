package com.meshlink.ui.media.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.document.DocumentCard
import com.meshlink.ui.media.models.MediaFilterChipsBar
import com.meshlink.ui.media.models.MediaFilterType
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Media Gallery supporting Grid / List view, date grouping, search, and multi-selection mode.
 */
@Composable
fun MediaGallery(
    mediaList: List<MediaUi>,
    onItemClick: (MediaUi) -> Unit,
    onDownloadClick: ((MediaUi) -> Unit)? = null,
    onShareClick: ((MediaUi) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MediaFilterType.ALL) }
    var isGridView by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    // Filter items
    val filteredItems = remember(mediaList, searchQuery, selectedFilter) {
        mediaList.filter { media ->
            val matchesSearch = searchQuery.isEmpty() ||
                    media.title.contains(searchQuery, ignoreCase = true) ||
                    media.senderName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                MediaFilterType.ALL -> true
                MediaFilterType.IMAGES -> media.mediaType == MediaType.IMAGE
                MediaFilterType.VIDEOS -> media.mediaType == MediaType.VIDEO
                MediaFilterType.AUDIO -> media.mediaType == MediaType.AUDIO
                MediaFilterType.VOICE_NOTES -> media.mediaType == MediaType.VOICE_NOTE
                MediaFilterType.DOCUMENTS -> media.mediaType == MediaType.DOCUMENT
                MediaFilterType.APK -> media.mediaType == MediaType.APK
                MediaFilterType.ZIP -> media.mediaType == MediaType.ZIP
                MediaFilterType.PDF -> media.mediaType == MediaType.PDF
                MediaFilterType.FAVORITES -> media.isFavorite
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background)
    ) {
        // Search & View Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaSearch(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.weight(1f)
            )

            // View Mode Toggle (Grid vs List)
            IconButton(onClick = { isGridView = !isGridView }) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle View Mode",
                    tint = MeshTheme.colors.primary
                )
            }
        }

        // Filter Chips Horizontal Scrollbar
        MediaFilterChipsBar(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // Main Content Area
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MeshTheme.colors.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No media or attachments found",
                        style = MeshTheme.customTypography.subtitle,
                        color = MeshTheme.colors.onSurfaceVariant
                    )
                }
            }
        } else if (isGridView) {
            MediaGrid(
                items = filteredItems,
                onItemClick = onItemClick,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                onItemToggleSelect = { media ->
                    selectedIds = if (selectedIds.contains(media.id)) {
                        selectedIds - media.id
                    } else {
                        selectedIds + media.id
                    }
                }
            )
        } else {
            // Detailed List View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredItems,
                    key = { item -> item.id }
                ) { item ->
                    DocumentCard(
                        media = item,
                        onOpenClick = onItemClick,
                        onDownloadClick = onDownloadClick,
                        onShareClick = onShareClick
                    )
                }
            }
        }
    }
}
