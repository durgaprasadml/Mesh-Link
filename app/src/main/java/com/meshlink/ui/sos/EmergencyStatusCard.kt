package com.meshlink.ui.sos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun EmergencyStatusCard(
    state: SosUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (state.status == SosStatus.DELIVERED || state.status == SosStatus.BROADCASTING) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "Cancel SOS",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else if (state.status == SosStatus.FAILED) {
                    TextButton(onClick = onRetry) {
                        Text(
                            text = "Retry",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Field 1: Status
            StatusGridRow(
                label = "Status",
                value = when (state.status) {
                    SosStatus.SAFE -> "Safe (Standby)"
                    SosStatus.BROADCASTING -> "Broadcasting Alert"
                    SosStatus.DELIVERED -> "Delivered to Responders"
                    SosStatus.FAILED -> "Transmission Failed"
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 2: Location
            StatusGridRow(
                label = "Location",
                value = if (state.latitude != null && state.longitude != null) {
                    String.format(Locale.US, "%.4f, %.4f", state.latitude, state.longitude)
                } else {
                    state.address ?: "Unavailable"
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 3: Accuracy
            StatusGridRow(
                label = "Accuracy",
                value = if (state.latitude != null) "GPS Fixed (~10m)" else if (state.isFetchingLocation) "Acquiring..." else "Unavailable"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 4: Nearby Responders
            StatusGridRow(
                label = "Nearby Responders",
                value = if (state.nearbyResponders.isNotEmpty()) "${state.nearbyResponders.size} devices" else "Scanning..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 5: Delivery Status
            StatusGridRow(
                label = "Delivery Status",
                value = when (state.status) {
                    SosStatus.SAFE -> "Not broadcasted"
                    SosStatus.BROADCASTING -> "Transmitting across mesh..."
                    SosStatus.DELIVERED -> "Confirmed (${state.relaysReached} hops)"
                    SosStatus.FAILED -> state.errorMessage ?: "Delivery failed"
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Field 6: Connection
            StatusGridRow(
                label = "Connection",
                value = when {
                    state.isBleEnabled && state.isWifiDirectEnabled -> "BLE + Wi-Fi Direct Mesh"
                    state.isBleEnabled -> "BLE Mesh Active"
                    state.isWifiDirectEnabled -> "Wi-Fi Direct Active"
                    else -> "Unavailable"
                }
            )
        }
    }
}

@Composable
private fun StatusGridRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

