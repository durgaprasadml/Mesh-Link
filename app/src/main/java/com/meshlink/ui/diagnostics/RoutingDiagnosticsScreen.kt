package com.meshlink.ui.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meshlink.domain.model.RouteEntry
import com.meshlink.domain.model.RouteState
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingDiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: RoutingDiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MeshScreen(
        topBar = {
            com.meshlink.ui.components.MeshTopAppBar(
                title = "Routing Diagnostics",
                onBackClick = onBackClick,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MeshSpacing.ScreenPadding),
            contentPadding = PaddingValues(bottom = MeshSpacing.ListBottomSpacing),
            verticalArrangement = Arrangement.spacedBy(MeshSpacing.CardSpacing)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HealthHeaderCard(health = uiState.networkHealth, meshSize = uiState.meshSize)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "RREQ Active", value = uiState.activeDiscoveryCount.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Pending Queue", value = uiState.pendingQueueSize.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Store & Forward", value = uiState.storeForwardCount.toString(), modifier = Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "Duplicate Cache", value = uiState.duplicateCacheSize.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Route Repairs", value = uiState.routeRepairCount.toString(), modifier = Modifier.weight(1f))
                    MetricCard(title = "Mesh Size", value = "${uiState.meshSize} Nodes", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Live Route Table (${uiState.routes.size} entries)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.routes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active routes in cache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.routes) { route ->
                    RouteRowItem(route = route)
                }
            }
        }
    }
}

@Composable
fun HealthHeaderCard(health: String, meshSize: Int) {
    val healthColor = when (health) {
        "HEALTHY" -> MeshTheme.colors.success
        "DEGRADED" -> MeshTheme.colors.warning
        else -> MeshTheme.colors.info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
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
                    text = "Mesh Network Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$meshSize active dynamic hop destination(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Surface(
                color = healthColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = health,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RouteRowItem(route: RouteEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dest: ${com.meshlink.util.MeshIdNormalizer.canonicalize(route.destinationId)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Score: ${route.score}/100",
                    fontWeight = FontWeight.Bold,
                    color = if (route.score >= 50) MeshTheme.colors.success else MeshTheme.colors.danger,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NextHop: ${com.meshlink.util.MeshIdNormalizer.canonicalize(route.nextHop)} (${route.hops} hop)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "RSSI: ${route.metrics.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Type: ${route.routeType.name} | Latency: ${route.metrics.averageLatencyMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "State: ${route.state.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (route.state) {
                        RouteState.ACTIVE -> MeshTheme.colors.success
                        RouteState.STALE -> MeshTheme.colors.warning
                        else -> MeshTheme.colors.offline
                    }
                )
            }
        }
    }
}
