package com.meshlink.ui.designsystem.components.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.glass.MeshGlassCard
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

@Composable
fun ConnectionStatusPill(
    status: String,
    modifier: Modifier = Modifier,
    activeNodesCount: Int = 0,
    rssiDbm: Int = -70
) {
    val colors = LocalMeshSemanticColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MeshStatusBadge(status = status, color = if (status.uppercase() == "CONNECTED") colors.meshConnected else colors.meshOffline)
        if (status.uppercase() == "CONNECTED") {
            Spacer(modifier = Modifier.width(8.dp))
            SignalMeter(rssiDbm = rssiDbm)
        }
    }
}

@Composable
fun MeshConnectionStatusPill(
    status: String,
    modifier: Modifier = Modifier,
    activeNodesCount: Int = 0,
    rssiDbm: Int = -70
) = ConnectionStatusPill(status = status, modifier = modifier, activeNodesCount = activeNodesCount, rssiDbm = rssiDbm)

@Composable
fun MeshStatusComponent(
    status: String,
    activeNodesCount: Int,
    transportType: String,
    rssiDbm: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalMeshSemanticColors.current

    MeshGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = if (status.uppercase() == "CONNECTED") colors.meshConnected else colors.meshOffline
                    MeshStatusBadge(status = status, color = dotColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mesh Network",
                        style = MeshTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$activeNodesCount Active Peers • Transport: $transportType",
                    style = MeshTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            SignalMeter(rssiDbm = rssiDbm)
        }
    }
}
