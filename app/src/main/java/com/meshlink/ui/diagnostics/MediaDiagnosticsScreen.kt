package com.meshlink.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meshlink.transfer.TransferMetrics
import com.meshlink.transfer.TransferState
import com.meshlink.transfer.TransportType
import com.meshlink.ui.components.MeshScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: MediaDiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MeshScreen(
        topBar = {
            TopAppBar(
                title = { Text("Media Transfer Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Overall Subsystem Health Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Media Subsystem Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Transport Mode: ${uiState.activeModeName} | Window: ${uiState.currentSlidingWindowSize} chunks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "PRODUCTION READY",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "Active Queue", value = uiState.activeTransfers.size.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Completed", value = uiState.completedTransfersCount.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Failed", value = uiState.failedTransfersCount.toString(), modifier = Modifier.weight(1f))
                }
            }

            item {
                val speedKb = uiState.averageSpeedBytesPerSec / 1024f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "Avg Speed", value = "${String.format("%.1f", speedKb)} KB/s", modifier = Modifier.weight(1f))
                    MetricCard(title = "Retries", value = uiState.totalRetries.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "CRC32 Errors", value = uiState.totalCrc32Errors.toString(), modifier = Modifier.weight(1f))
                }
            }

            item {
                val compPercent = ((1f - uiState.averageCompressionRatio) * 100f).coerceAtLeast(0f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "Saved via Comp", value = "${String.format("%.0f", compPercent)}%", modifier = Modifier.weight(1f))
                    MetricCard(title = "BLE Transfers", value = uiState.bleTransfersCount.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Wi-Fi Transfers", value = uiState.wifiTransfersCount.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Live Active Transfers (${uiState.activeTransfers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.activeTransfers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active media transfers in progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.activeTransfers, key = { it.transferId }) { item ->
                    TransferRowItem(
                        metrics = item,
                        onPause = { viewModel.pauseTransfer(item.transferId) },
                        onResume = { viewModel.resumeTransfer(item.transferId) },
                        onCancel = { viewModel.cancelTransfer(item.transferId) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransferRowItem(
    metrics: TransferMetrics,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${metrics.transferId.take(12)}...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Surface(
                    color = when (metrics.activeTransport) {
                        TransportType.WIFI_DIRECT -> Color(0xFF1565C0)
                        TransportType.HYBRID -> Color(0xFF6A1B9A)
                        else -> Color(0xFF2E7D32)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = metrics.activeTransport.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val speedKb = metrics.currentSpeedBytesPerSec / 1024f
            val progressPercent = (metrics.progress * 100).toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status: ${metrics.status.name} ($progressPercent%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Speed: ${String.format("%.1f", speedKb)} KB/s",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { metrics.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chunk: ${metrics.currentChunk}/${metrics.totalChunks} | Retries: ${metrics.retries}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (metrics.status == TransferState.SENDING || metrics.status == TransferState.RECEIVING) {
                        IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                        }
                    } else if (metrics.status == TransferState.PAUSED) {
                        IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
