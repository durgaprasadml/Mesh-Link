package com.meshlink.ui.designsystem.catalog

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.badges.MeshAvatar
import com.meshlink.ui.designsystem.components.badges.MeshBadge
import com.meshlink.ui.designsystem.components.badges.MeshChip
import com.meshlink.ui.designsystem.components.buttons.EmergencyButton
import com.meshlink.ui.designsystem.components.buttons.MeshButton
import com.meshlink.ui.designsystem.components.buttons.MeshButtonVariant
import com.meshlink.ui.designsystem.components.buttons.MeshFAB
import com.meshlink.ui.designsystem.components.buttons.MeshIconButton
import com.meshlink.ui.designsystem.components.cards.HeroCard
import com.meshlink.ui.designsystem.components.cards.MetricCard
import com.meshlink.ui.designsystem.components.cards.QuickActionTile
import com.meshlink.ui.designsystem.components.glass.MeshGlassCard
import com.meshlink.ui.designsystem.components.inputs.MeshInputField
import com.meshlink.ui.designsystem.components.inputs.MeshSearchBar
import com.meshlink.ui.designsystem.components.navigation.MeshNavigationBar
import com.meshlink.ui.designsystem.components.navigation.MeshNavigationItem
import com.meshlink.ui.designsystem.components.shared.HopBadge
import com.meshlink.ui.designsystem.components.shared.MeshListItem
import com.meshlink.ui.designsystem.components.shared.MeshStatusBadge
import com.meshlink.ui.designsystem.components.shared.SignalMeter
import com.meshlink.ui.designsystem.theme.MeshTheme

@Composable
fun DesignSystemPreviewScreen() {
    val colors = MeshTheme.colors
    val scrollState = rememberScrollState()

    var searchQuery by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var navSelected by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Mesh-Link 2026 Design System Catalog",
            style = MeshTheme.typography.titleLarge,
            color = colors.textPrimary
        )

        // 1. Telemetry Status
        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    MeshStatusBadge(status = "CONNECTED")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Active Peers: 14 Nodes", color = colors.textSecondary)
                }
                SignalMeter(rssiDbm = -62)
            }
        }

        // 2. Tactile Cards & Hero Surfaces
        HeroCard(
            title = "Aether Mesh Topology",
            subtitle = "Direct peer-to-peer connection via Bluetooth LE & Wi-Fi Direct."
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(label = "Mesh RSSI", value = "-62", unit = "dBm")
            MetricCard(label = "Hop Distance", value = "1", unit = "Direct")
        }

        QuickActionTile(
            title = "Broadcast SOS Signal",
            subtitle = "High-priority emergency beacon to all mesh peers",
            icon = Icons.Default.Send,
            onClick = {}
        )

        MeshListItem(
            headline = "Node Alpha-7",
            subhead = "Distance: 1 Hop • BLE 5.3",
            leadingContent = { MeshAvatar(name = "Alpha") }
        )

        // 3. Button System
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeshButton(text = "Primary", onClick = {})
            MeshButton(text = "Outlined", onClick = {}, variant = MeshButtonVariant.OUTLINED)
            EmergencyButton(text = "Emergency", onClick = {})
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            MeshIconButton(icon = Icons.Default.Add, contentDescription = "Add", onClick = {})
            Spacer(modifier = Modifier.width(12.dp))
            MeshFAB(icon = Icons.Default.Hub, contentDescription = "Mesh", label = "Connect Node", onClick = {})
        }

        // 4. Inputs
        MeshSearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
        MeshInputField(value = textInput, onValueChange = { textInput = it }, placeholder = "Enter alias...", label = "PEER ALIAS")

        // 5. Badges & Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeshBadge(text = "Aether Grid")
            HopBadge(hopCount = 1)
            MeshChip(label = "Encrypted", selected = true, onClick = {})
        }

        // 6. Navigation Bar
        MeshNavigationBar {
            MeshNavigationItem(selected = navSelected == 0, onClick = { navSelected = 0 }, icon = Icons.Default.Hub, label = "Home")
            MeshNavigationItem(selected = navSelected == 1, onClick = { navSelected = 1 }, icon = Icons.Default.Bluetooth, label = "Nearby")
            MeshNavigationItem(selected = navSelected == 2, onClick = { navSelected = 2 }, icon = Icons.Default.Send, label = "Chats")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(name = "Light Mode Preview", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun DesignSystemPreviewLight() {
    MeshTheme(themeMode = "LIGHT") {
        DesignSystemPreviewScreen()
    }
}

@Preview(name = "Dark Mode Preview", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DesignSystemPreviewDark() {
    MeshTheme(themeMode = "DARK") {
        DesignSystemPreviewScreen()
    }
}

@Preview(name = "AMOLED Preview", showBackground = true)
@Composable
fun DesignSystemPreviewAmoled() {
    MeshTheme(themeMode = "DARK", amoledDark = true) {
        DesignSystemPreviewScreen()
    }
}
