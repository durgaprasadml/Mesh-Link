package com.meshlink.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.media.gallery.MediaGallery
import com.meshlink.ui.media.models.MediaUi
import com.meshlink.ui.media.models.TransferStatisticsUi
import com.meshlink.ui.media.models.TransferUi
import com.meshlink.ui.media.transfer.TransferHistoryList
import com.meshlink.ui.media.transfer.TransferProgressCard
import com.meshlink.ui.media.transfer.TransferStatisticsDashboard
import com.meshlink.ui.media.viewer.MediaViewer

/**
 * Main rich Media, File Transfer & Rich Content Experience Screen.
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
    var selectedTab by remember { mutableStateOf(0) } // 0: Gallery, 1: Transfers, 2: Statistics
    var activeViewerMedia by remember { mutableStateOf<MediaUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Media & File Sharing",
                        style = MeshTheme.customTypography.title,
                        color = MeshTheme.colors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MeshTheme.colors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MeshTheme.colors.surface
                )
            )
        },
        containerColor = MeshTheme.colors.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Active Live Transfer Progress Bar (If any active transfer running)
            if (activeTransfers.isNotEmpty()) {
                val currentActive = activeTransfers.first()
                TransferProgressCard(
                    transfer = currentActive,
                    onPauseClick = onPauseTransfer,
                    onResumeClick = onResumeTransfer,
                    onCancelClick = onCancelTransfer,
                    onRetryClick = onRetryTransfer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Top Navigation Tabs (Gallery, Transfers, Telemetry Statistics)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MeshTheme.colors.surface,
                contentColor = MeshTheme.colors.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Gallery (${mediaList.size})", style = MeshTheme.customTypography.subtitle) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Transfers (${activeTransfers.size})", style = MeshTheme.customTypography.subtitle) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Telemetry", style = MeshTheme.customTypography.subtitle) }
                )
            }

            // Body Content based on active tab
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> {
                        MediaGallery(
                            mediaList = mediaList,
                            onItemClick = { media -> activeViewerMedia = media }
                        )
                    }
                    1 -> {
                        TransferHistoryList(
                            transfers = activeTransfers,
                            onRetryClick = onRetryTransfer,
                            onCancelClick = onCancelTransfer
                        )
                    }
                    2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            TransferStatisticsDashboard(
                                stats = statistics
                            )
                        }
                    }
                }
            }
        }
    }

    // Universal Lightbox Media Viewer Overlay
    if (activeViewerMedia != null) {
        MediaViewer(
            media = activeViewerMedia,
            onClose = { activeViewerMedia = null }
        )
    }
}
