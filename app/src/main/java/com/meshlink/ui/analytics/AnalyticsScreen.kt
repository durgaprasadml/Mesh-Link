package com.meshlink.ui.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.analytics.data.LogType
import com.meshlink.analytics.data.MeshStats
import com.meshlink.analytics.data.RelayLogEntry
import com.meshlink.ui.designsystem.theme.MeshTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Mesh Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumLarge)
        ) {
            // ── Delivery Ring ──
            item {
                DeliveryRateCard(uiState.stats)
            }

            // ── Stats Grid ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Send,
                        label = "Sent",
                        value = uiState.stats.packetsSent.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DoneAll,
                        label = "Delivered",
                        value = uiState.stats.packetsDelivered.toString(),
                        color = MeshTheme.colors.success
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SyncAlt,
                        label = "Relayed",
                        value = uiState.stats.packetsRelayed.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "Dropped",
                        value = uiState.stats.packetsFailed.toString(),
                        color = MeshTheme.colors.error
                    )
                }
            }

            // ── Avg Hop Count + Active Nodes ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timeline,
                        label = "Avg Hops",
                        value = String.format(java.util.Locale.US, "%.1f", uiState.stats.avgHopCount),
                        color = MeshTheme.colors.warning
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        label = "Active Nodes",
                        value = uiState.activeNodes.size.toString(),
                        color = MeshTheme.colors.info
                    )
                }
            }

            // ── Hop Distribution Chart ──
            if (uiState.hopDistribution.isNotEmpty()) {
                item {
                    HopDistributionCard(uiState.hopDistribution)
                }
            }

            // ── Active Nodes List ──
            if (uiState.activeNodes.isNotEmpty()) {
                item {
                    ActiveNodesCard(uiState.activeNodes)
                }
            }

            // ── Relay Log ──
            item {
                Text("Recent Activity", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            }

            item {
                AnimatedContent<Boolean>(
                    targetState = uiState.recentLog.isEmpty(),
                    label = "recent_log_empty_state_transition"
                ) { isEmpty ->
                    if (isEmpty) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(MeshTheme.spacing.huge),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                                ) {
                                    Text("No activity yet. Start messaging!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
                        ) {
                            uiState.recentLog.forEach { entry ->
                                RelayLogCard(entry)
                            }
                        }
                    }
                }
            }
            // Bottom spacer
            item { Spacer(modifier = Modifier.height(MeshTheme.spacing.huge)) }
        }
    }
}

// ────────── Delivery Rate Ring ──────────

@Composable
fun DeliveryRateCard(stats: MeshStats) {
    val animatedProgress = animateFloatAsState(
        targetValue = if (stats.packetsSent > 0) stats.deliveryRate / 100f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "deliveryAnim"
    )

    val ringColor = when {
        stats.deliveryRate >= 80f -> MeshTheme.colors.success
        stats.deliveryRate >= 50f -> MeshTheme.colors.warning
        stats.deliveryRate > 0f -> MeshTheme.colors.error
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(MeshTheme.spacing.extraLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated ring
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    // Background ring
                    drawArc(
                        color = ringColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                    // Progress ring
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${stats.deliveryRate.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ringColor
                )
            }

            Spacer(modifier = Modifier.width(MeshTheme.spacing.extraLarge))

            Column {
                Text(
                    "Delivery Success",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Text(
                    "${stats.packetsDelivered} of ${stats.packetsSent} packets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))
                Text(
                    "${stats.packetsRelayed} relayed through mesh",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// ────────── Stat Card ──────────

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
                .padding(MeshTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, contentDescription = label,
                tint = color, modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumSmall))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ────────── Hop Distribution Bar Chart ──────────

@Composable
fun HopDistributionCard(distribution: Map<Int, Int>) {
    val maxCount = distribution.values.maxOrNull() ?: 1
    val barColors = listOf(
        MaterialTheme.colorScheme.primary,
        MeshTheme.colors.success,
        MeshTheme.colors.warning,
        MeshTheme.colors.error,
        MaterialTheme.colorScheme.secondary,
        MeshTheme.colors.info
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.large)) {
            Text(
                "Hop Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MeshTheme.spacing.mediumLarge))

            distribution.entries.sortedBy { it.key }.forEachIndexed { idx, (hopCount, count) ->
                val fraction = count.toFloat() / maxCount
                val color = barColors[idx % barColors.size]

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = MeshTheme.spacing.small)
                ) {
                    Text(
                        "${hopCount}h",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(28.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(MeshTheme.spacing.large)
                            .clip(MeshTheme.shapes.pill)
                            .background(color.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(MeshTheme.shapes.pill)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(color, color.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(36.dp).padding(start = MeshTheme.spacing.mediumSmall),
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

// ────────── Active Nodes ──────────

@Composable
fun ActiveNodesCard(nodes: Set<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(MeshTheme.spacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = "WiFi icon", tint = MeshTheme.colors.success, modifier = Modifier.size(MeshTheme.spacing.large))
                Spacer(modifier = Modifier.width(MeshTheme.spacing.mediumSmall))
                Text(
                    "Active Mesh Nodes (${nodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(MeshTheme.spacing.medium))

            // Node chips
            val nodesList = nodes.toList()
            val rows = nodesList.chunked(3)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MeshTheme.spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MeshTheme.spacing.mediumSmall)
                ) {
                    row.forEach { nodeId ->
                        Surface(
                            shape = MeshTheme.shapes.pill,
                            color = MeshTheme.colors.success.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulsing dot
                                Box(
                                    modifier = Modifier
                                        .size(MeshTheme.spacing.mediumSmall)
                                        .clip(CircleShape)
                                        .background(MeshTheme.colors.success)
                                )
                                Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                                Text(
                                    com.meshlink.util.MeshIdNormalizer.canonicalize(nodeId),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────── Relay Log Entry ──────────

@Composable
fun RelayLogCard(entry: RelayLogEntry) {
    val (bgColor, accentColor) = when (entry.type) {
        LogType.RELAY -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f) to MaterialTheme.colorScheme.secondary
        LogType.FAILURE -> MeshTheme.colors.error.copy(alpha = 0.06f) to MeshTheme.colors.error
        LogType.SECURITY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) to MaterialTheme.colorScheme.primary
        LogType.SOS -> MeshTheme.colors.danger.copy(alpha = 0.1f) to MeshTheme.colors.danger
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(MeshTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(MeshTheme.spacing.small, MeshTheme.spacing.huge)
                    .clip(MeshTheme.shapes.extraSmall)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(MeshTheme.spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.event,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    entry.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                formatLogTime(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private fun formatLogTime(ts: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ts))
}
