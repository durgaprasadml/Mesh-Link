package com.meshlink.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.domain.model.RouteEntry
import com.meshlink.routing.api.Router
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.transport.HybridMode
import com.meshlink.transport.HybridTransport
import com.meshlink.transport.HybridTransportMetrics
import com.meshlink.wifi.data.WifiDirectManager
import com.meshlink.wifi.data.WifiSocketTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val isP2pEnabled: Boolean = false,
    val isDiscovering: Boolean = false,
    val connectionState: String = "DISCONNECTED",
    val isGroupOwner: Boolean = false,
    val groupOwnerIp: String = "N/A",
    val localIp: String = "N/A",
    val hybridMode: HybridMode = HybridMode.BLE_ONLY,
    val metrics: HybridTransportMetrics = HybridTransportMetrics(),
    val blePeerCount: Int = 0,
    val wifiPeerCount: Int = 0,
    val connectedWifiPeers: List<String> = emptyList(),
    val activeRoutes: List<RouteEntry> = emptyList(),
    val isRelayActive: Boolean = true
)

@HiltViewModel
class WifiDiagnosticsViewModel @Inject constructor(
    val wifiDirectManager: WifiDirectManager,
    val wifiSocketTransport: WifiSocketTransport,
    val hybridTransport: HybridTransport,
    val router: Router
) : ViewModel() {

    private val p2pInfoFlow = combine(
        wifiDirectManager.isP2pEnabled,
        wifiDirectManager.isDiscovering,
        wifiDirectManager.connectionState,
        wifiDirectManager.isGroupOwner,
        wifiDirectManager.groupOwnerAddress
    ) { isP2p, isDisc, connState, isGo, goIp ->
        listOf(isP2p, isDisc, connState, isGo, goIp)
    }

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        p2pInfoFlow,
        wifiDirectManager.localIpAddress,
        hybridTransport.activeMode,
        hybridTransport.metrics,
        wifiSocketTransport.connectedPeersFlow
    ) { p2pList, localIp, mode, metrics, wifiPeers ->
        val isP2p = p2pList[0] as Boolean
        val isDisc = p2pList[1] as Boolean
        val connState = p2pList[2] as com.meshlink.wifi.data.WifiP2pConnectionState
        val isGo = p2pList[3] as Boolean
        val goIp = p2pList[4] as String?

        DiagnosticsUiState(
            isP2pEnabled = isP2p,
            isDiscovering = isDisc,
            connectionState = connState.name,
            isGroupOwner = isGo,
            groupOwnerIp = goIp ?: "N/A",
            localIp = localIp ?: "N/A",
            hybridMode = mode,
            metrics = metrics,
            blePeerCount = (hybridTransport.connectedPeers - wifiPeers).size,
            wifiPeerCount = wifiPeers.size,
            connectedWifiPeers = wifiPeers.toList(),
            activeRoutes = router.routeTable.values.toList(),
            isRelayActive = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiagnosticsUiState())

    fun triggerPeerDiscovery() {
        wifiDirectManager.discoverPeers()
    }

    fun triggerGroupCreation() {
        wifiDirectManager.createGroup()
    }

    fun triggerDisconnect() {
        wifiDirectManager.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: WifiDiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MeshScreen(
        topBar = {
            TopAppBar(
                title = { Text("Wi-Fi & Hybrid Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.triggerPeerDiscovery() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Discovery")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            contentPadding = PaddingValues(bottom = MeshTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            item {
                Text(
                    text = "Hybrid Transport Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                StatusCard(
                    title = "Active Transport Mode",
                    statusText = uiState.hybridMode.name,
                    icon = Icons.Default.Hub,
                    highlight = uiState.hybridMode == HybridMode.HYBRID_ACTIVE
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricCard(title = "BLE Peers", value = "${uiState.blePeerCount}", icon = Icons.Default.CellTower)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricCard(title = "Wi-Fi Direct Peers", value = "${uiState.wifiPeerCount}", icon = Icons.Default.Wifi)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transport Performance & Failover Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        DiagnosticRow(label = "BLE Packets Sent", value = "${uiState.metrics.totalPacketsSentBle}")
                        DiagnosticRow(label = "Wi-Fi Direct Packets Sent", value = "${uiState.metrics.totalPacketsSentWifi}")
                        DiagnosticRow(label = "Automatic Upgrades", value = "${uiState.metrics.upgradeCount}")
                        DiagnosticRow(label = "Automatic Downgrades", value = "${uiState.metrics.downgradeCount}")
                        DiagnosticRow(label = "Wi-Fi Fallbacks to BLE", value = "${uiState.metrics.fallbackCount}")
                        DiagnosticRow(label = "Total Packet Retries", value = "${uiState.metrics.retryCount}")
                        DiagnosticRow(label = "Cumulative Throughput", value = "${uiState.metrics.throughputBps} bytes")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wi-Fi Direct P2P Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagnosticRow(label = "Wi-Fi P2P Enabled", value = if (uiState.isP2pEnabled) "Yes" else "No")
                        DiagnosticRow(label = "Peer Discovery Active", value = if (uiState.isDiscovering) "Scanning..." else "Idle")
                        DiagnosticRow(label = "P2P Connection State", value = uiState.connectionState)
                        DiagnosticRow(label = "Role", value = if (uiState.isGroupOwner) "Group Owner (GO)" else "Client Node")
                        DiagnosticRow(label = "Group Owner IP", value = uiState.groupOwnerIp)
                        DiagnosticRow(label = "Local P2P IP", value = uiState.localIp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerPeerDiscovery() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Discover Peers", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { viewModel.triggerGroupCreation() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create GO Group", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { viewModel.triggerDisconnect() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Socket Connections (${uiState.connectedWifiPeers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.connectedWifiPeers.isEmpty()) {
                item {
                    Text(
                        "No active Wi-Fi Direct socket connections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.connectedWifiPeers.size) { idx ->
                    val peerIp = uiState.connectedWifiPeers[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Connected Peer IP: $peerIp", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Port: 8888 • Protocol: TCP Frame Stream", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, statusText: String, icon: ImageVector, highlight: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = MeshTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
