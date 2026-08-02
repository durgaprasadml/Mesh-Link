package com.meshlink.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun BatteryImpact(
    batteryUi: BatteryImpactUi,
    modifier: Modifier = Modifier
) {
    val impactColor = when (batteryUi.impactLevel.uppercase()) {
        "LOW" -> MeshTheme.colors.success
        "MEDIUM", "MODERATE" -> MeshTheme.colors.warning
        else -> MeshTheme.colors.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MeshTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = MeshTheme.elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(MeshTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery Telemetry",
                        tint = impactColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(MeshTheme.spacing.small))
                    Text(
                        text = "Battery & Power Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = impactColor.copy(alpha = 0.15f),
                    shape = MeshTheme.shapes.pill
                ) {
                    Text(
                        text = "${batteryUi.impactLevel} IMPACT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = impactColor,
                        modifier = Modifier.padding(horizontal = MeshTheme.spacing.medium, vertical = MeshTheme.spacing.small)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Estimated Power Impact",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = batteryUi.batteryLevelText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Radio Duty Cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (batteryUi.isPowerSaveActive) "POWER SAVER" else "ADAPTIVE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.small)) {
                Text(
                    text = "Transport Duty Allocation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MeshTheme.shapes.pill)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(batteryUi.bleUsagePercent.toFloat().coerceAtLeast(1f))
                            .background(MeshTheme.colors.info)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(batteryUi.wifiUsagePercent.toFloat().coerceAtLeast(1f))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BLE Mesh (${batteryUi.bleUsagePercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshTheme.colors.info
                    )
                    Text(
                        text = "Wi-Fi Direct (${batteryUi.wifiUsagePercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
