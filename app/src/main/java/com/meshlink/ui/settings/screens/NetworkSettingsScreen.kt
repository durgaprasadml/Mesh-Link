package com.meshlink.ui.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.settings.SettingsItemRow
import com.meshlink.ui.designsystem.theme.MeshTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Network & Transport") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MeshTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MeshTheme.spacing.large)
        ) {
            item {
                Text("Transport Protocols", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        var bleEnabled by remember { mutableStateOf(true) }
                        SettingsItemRow(
                            title = "Bluetooth Low Energy (BLE)",
                            subtitle = "Use for low power, short-range discovery.",
                            icon = Icons.Default.Bluetooth,
                            trailingContent = { Switch(checked = bleEnabled, onCheckedChange = { bleEnabled = it }) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        var wifiEnabled by remember { mutableStateOf(true) }
                        SettingsItemRow(
                            title = "Wi-Fi Direct",
                            subtitle = "Use for high-bandwidth data transfer.",
                            icon = Icons.Default.Wifi,
                            trailingContent = { Switch(checked = wifiEnabled, onCheckedChange = { wifiEnabled = it }) }
                        )
                    }
                }
            }

            item {
                Text("Mesh Behavior", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(MeshTheme.spacing.small))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MeshTheme.shapes.large
                ) {
                    Column {
                        var relayEnabled by remember { mutableStateOf(true) }
                        SettingsItemRow(
                            title = "Act as Relay Node",
                            subtitle = "Help route messages for other peers. Uses more battery.",
                            icon = Icons.Default.Memory,
                            trailingContent = { Switch(checked = relayEnabled, onCheckedChange = { relayEnabled = it }) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        var backgroundEnabled by remember { mutableStateOf(true) }
                        SettingsItemRow(
                            title = "Background Discovery",
                            subtitle = "Keep scanning when app is closed.",
                            trailingContent = { Switch(checked = backgroundEnabled, onCheckedChange = { backgroundEnabled = it }) }
                        )
                    }
                }
            }
        }
    }
}
