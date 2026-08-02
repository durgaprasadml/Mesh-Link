package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.models.MediaFilterType
import com.meshlink.ui.media.models.MediaType
import com.meshlink.ui.media.models.MediaUi
import com.meshlink.ui.media.models.TransferStatisticsUi
import com.meshlink.ui.media.models.TransferUi
import com.meshlink.ui.media.viewer.MediaViewer

/**
 * Main Production-Ready Media Sharing, Attachments & File Transfer Experience Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshMediaScreen(
    mediaList: List<MediaUi>,
    activeTransfers: List<TransferUi>,
    statistics: TransferStatisticsUi,
    onBack: () -> Unit,
    onPauseTransfer: ((String) -> Unit)? = null,
    onResumeTransfer: ((String) -> Unit)? = null,
    onCancelTransfer: ((String) -> Unit)? = null,
    onRetryTransfer: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MediaFilterType.ALL) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Media Grid, 1: Files, 2: Transfers, 3: Storage
    var activeViewerMedia by remember { mutableStateOf<MediaUi?>(null) }
    var selectedTransferForSheet by remember { mutableStateOf<TransferUi?>(null) }

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    // Filter media items by search query and filter chips
    val filteredMediaList = remember(mediaList, searchQuery, selectedFilter) {
        mediaList.filter { media ->
            val matchesSearch = searchQuery.isBlank() ||
                    media.title.contains(searchQuery, ignoreCase = true) ||
                    media.senderName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                MediaFilterType.IMAGES -> media.mediaType == MediaType.IMAGE
                MediaFilterType.VIDEOS -> media.mediaType == MediaType.VIDEO
                MediaFilterType.AUDIO -> media.mediaType == MediaType.AUDIO || media.mediaType == MediaType.VOICE_NOTE
                MediaFilterType.DOCUMENTS -> media.mediaType == MediaType.DOCUMENT || media.mediaType == MediaType.PDF
                MediaFilterType.APK -> media.mediaType == MediaType.APK
                MediaFilterType.ZIP -> media.mediaType == MediaType.ZIP
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val imagesAndVideos = remember(filteredMediaList) {
        filteredMediaList.filter { it.mediaType == MediaType.IMAGE || it.mediaType == MediaType.VIDEO }
    }

    val filesAndDocs = remember(filteredMediaList) {
        filteredMediaList.filter { it.mediaType != MediaType.IMAGE && it.mediaType != MediaType.VIDEO }
    }

    Scaffold(
        topBar = {
            MediaTopBar(
                title = "Media & Files",
                subtitle = "Shared over Mesh Network",
                onBackClick = onBack,
                onSearchClick = { /* Focus search */ },
                onFilterClick = { /* Open filter drawer */ },
                onSortClick = { /* Open sort menu */ },
                onMoreClick = { /* Open options menu */ }
            )
        },
        containerColor = MeshTheme.colors.background,
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Transfer Summary Dashboard Header
            TransferSummary(
                statistics = statistics,
                activeTransfersCount = activeTransfers.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Search Bar & Filter Chips Bar
            MediaSearch(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            MediaFilters(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // Navigation Tabs (Media, Files, Active Queue, Storage)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MeshTheme.colors.surface,
                contentColor = MeshTheme.colors.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Media (${imagesAndVideos.size})", style = MeshTheme.customTypography.subtitle) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Files (${filesAndDocs.size})", style = MeshTheme.customTypography.subtitle) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Queue (${activeTransfers.size})", style = MeshTheme.customTypography.subtitle) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Storage", style = MeshTheme.customTypography.subtitle) }
                )
            }

            // Screen Content Body
            Box(modifier = Modifier.fillMaxSize()) {
                if (isTabletOrLandscape) {
                    // Two-Pane Adaptive Layout for Tablets / Foldables / Landscape
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            when (selectedTab) {
                                0 -> SharedMediaGrid(
                                    mediaList = imagesAndVideos,
                                    onItemClick = { media -> activeViewerMedia = media }
                                )
                                else -> SharedFiles(
                                    filesList = filesAndDocs,
                                    onFileClick = { media -> activeViewerMedia = media }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            Column {
                                StorageOverview(
                                    usedBytes = statistics.totalTransferredBytes,
                                    totalBytes = 64_000_000_000L
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Active Transfer Queue",
                                    style = MeshTheme.customTypography.title,
                                    color = MeshTheme.colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TransferQueue(
                                    transfers = activeTransfers,
                                    onPauseClick = onPauseTransfer,
                                    onResumeClick = onResumeTransfer,
                                    onCancelClick = onCancelTransfer,
                                    onRetryClick = onRetryTransfer,
                                    onItemClick = { transfer -> selectedTransferForSheet = transfer }
                                )
                            }
                        }
                    }
                } else {
                    // Single Column Layout for Phones
                    when (selectedTab) {
                        0 -> SharedMediaGrid(
                            mediaList = imagesAndVideos,
                            onItemClick = { media -> activeViewerMedia = media }
                        )
                        1 -> SharedFiles(
                            filesList = filesAndDocs,
                            onFileClick = { media -> activeViewerMedia = media }
                        )
                        2 -> TransferQueue(
                            transfers = activeTransfers,
                            onPauseClick = onPauseTransfer,
                            onResumeClick = onResumeTransfer,
                            onCancelClick = onCancelTransfer,
                            onRetryClick = onRetryTransfer,
                            onItemClick = { transfer -> selectedTransferForSheet = transfer },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        3 -> Box(modifier = Modifier.padding(16.dp)) {
                            StorageOverview(
                                usedBytes = statistics.totalTransferredBytes,
                                totalBytes = 64_000_000_000L
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Transfer Detail Sheet
    if (selectedTransferForSheet != null) {
        TransferDetailSheet(
            transfer = selectedTransferForSheet,
            onDismiss = { selectedTransferForSheet = null },
            onRetryTransfer = { transfer -> onRetryTransfer?.invoke(transfer.transferId) }
        )
    }

    // Universal Lightbox Media Viewer Overlay
    if (activeViewerMedia != null) {
        MediaViewer(
            media = activeViewerMedia,
            onClose = { activeViewerMedia = null }
        )
    }
}
