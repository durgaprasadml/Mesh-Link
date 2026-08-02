package com.meshlink.ui.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.domain.model.BleDevice

/**
 * MeshDiscoveryHero — Central visual hero component occupying ~35-40% screen height.
 */
@Composable
fun MeshDiscoveryHero(
    devices: List<BleDevice>,
    selectedAddress: String?,
    isScanning: Boolean,
    onNodeSelected: (BleDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = devices.size
    val connectedCount = devices.count { it.isConnected }
    val relayCount = devices.count { (it.capabilities.toInt() and 0x01) != 0 }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live Radar Canvas Visualization
            MeshRadar(
                devices = devices,
                selectedAddress = selectedAddress,
                isScanning = isScanning,
                onNodeSelected = onNodeSelected,
                modifier = Modifier.fillMaxSize()
            )

            // Top Status Overlay Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Scanning Status Pill
                ScanStatusPill(isScanning = isScanning)

                // Quick Statistics Badge
                HeroStatBadge(
                    totalCount = totalCount,
                    connectedCount = connectedCount,
                    relayCount = relayCount
                )
            }
        }
    }
}

@Composable
private fun ScanStatusPill(isScanning: Boolean) {
    val scanAlpha by DiscoveryAnimations.rememberScanBreathingAlpha()

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScanning) MaterialTheme.colorScheme.primary.copy(alpha = scanAlpha)
                        else MaterialTheme.colorScheme.primary
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isScanning) "Scanning..." else "Radar Active",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HeroStatBadge(
    totalCount: Int,
    connectedCount: Int,
    relayCount: Int
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$totalCount Found",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            if (connectedCount > 0) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "$connectedCount Connected",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00F59B)
                )
            }
        }
    }
}
