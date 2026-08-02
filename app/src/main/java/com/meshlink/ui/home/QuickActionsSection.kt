package com.meshlink.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionsSection(
    onNavigateToNearby: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    onStartConversation: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    nearbyCount: Int = 0
) {
    val items = listOf(
        QuickActionCardItem(
            title = "Nearby",
            subtitle = "Discover",
            icon = Icons.Default.Wifi,
            iconBg = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onNavigateToNearby
        ),
        QuickActionCardItem(
            title = "Broadcast",
            subtitle = "Post message",
            icon = Icons.Default.Campaign,
            iconBg = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onNavigateToBroadcast
        ),
        QuickActionCardItem(
            title = "SOS",
            subtitle = "Emergency",
            icon = Icons.Default.Warning,
            iconBg = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            onClick = onNavigateToSos
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            QuickActionCard(
                item = item,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
