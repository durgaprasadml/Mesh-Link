package com.meshlink.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.components.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun NetworkStatistics(
    devices: List<NearbyDeviceUiState>,
    packetCount: Int,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val totalCount = devices.size
    val connectedCount = devices.count { it.isConnected }
    val avgRssi = if (devices.isNotEmpty()) (devices.sumOf { it.rssi } / devices.size) else 0
    val strongestPeer = devices.maxByOrNull { it.rssi }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MeshSpacing.SM)
    ) {
        StatCard(
            label = "Nearby Nodes",
            value = totalCount.toString(),
            color = MeshTheme.colors.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Active Links",
            value = connectedCount.toString(),
            color = MeshTheme.colors.connected,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Packets Processed",
            value = packetCount.toString(),
            color = MeshTheme.colors.broadcasting,
            modifier = Modifier.weight(1f)
        )
        if (devices.isNotEmpty()) {
            StatCard(
                label = "Avg Signal",
                value = "$avgRssi dBm",
                color = Color(Color(SignalStrength.fromRssi(avgRssi).quality.colorHex).value),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    MeshGlassCard(
        modifier = modifier,
        cornerRadius = 12.dp,
        glowColor = color,
        glowRadius = 60f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
