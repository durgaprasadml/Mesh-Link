package com.meshlink.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

@Composable
fun AppShortcutsSection(
    onShortcutClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        ShortcutItem("new_chat", "New Chat", Icons.Default.AddComment),
        ShortcutItem("nearby", "Nearby Nodes", Icons.Default.Devices),
        ShortcutItem("broadcast", "Broadcast", Icons.Default.Radio),
        ShortcutItem("sos", "Emergency SOS", Icons.Default.Warning),
        ShortcutItem("scan", "Scan BLE", Icons.Default.CellTower)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "App Launcher Quick Shortcuts",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            shortcuts.forEach { shortcut ->
                AppShortcutItemCard(
                    item = shortcut,
                    onClick = { onShortcutClick(shortcut.id) }
                )
            }
        }
    }
}

private data class ShortcutItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
private fun AppShortcutItemCard(
    item: ShortcutItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
