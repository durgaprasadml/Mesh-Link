package com.meshlink.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiagnosticsSettings(
    onExportLogs: () -> Unit = {},
    onNavigateToDeveloperOptions: () -> Unit = {},
    onNavigateToNetworkDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Diagnostics & Network Insights",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.Analytics,
                    title = "Network Analytics & Metrics",
                    subtitle = "Packet loss rates, signal RSSI, hop count averages",
                    statusChipText = "Healthy",
                    statusChipColor = MaterialTheme.colorScheme.primaryContainer,
                    statusChipTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToNetworkDiagnostics
                )

                SettingsRow(
                    icon = Icons.Default.Route,
                    title = "Mesh Routing Table Status",
                    subtitle = "AODV multi-hop routing paths & peer distance metrics",
                    onClick = onNavigateToNetworkDiagnostics
                )

                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                    title = "BLE & Wi-Fi Direct Diagnostics",
                    subtitle = "Inspect GATT service discovery & P2P socket connections",
                    onClick = onNavigateToNetworkDiagnostics
                )

                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = "Export Diagnostic Logs",
                    subtitle = "Dump encrypted event log file for protocol debugging",
                    onClick = onExportLogs
                )

                SettingsRow(
                    icon = Icons.Default.Code,
                    title = "Developer Options",
                    subtitle = "Advanced transport parameters, beacon rate & log verbosity",
                    onClick = onNavigateToDeveloperOptions,
                    showDivider = false
                )
            }
        }
    }
}
