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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.components.badges.MeshAvatar
import com.meshlink.ui.designsystem.components.badges.MeshBadge
import com.meshlink.ui.designsystem.components.badges.MeshHopBadge
import com.meshlink.ui.designsystem.components.badges.MeshNotificationBadge
import com.meshlink.ui.designsystem.components.badges.MeshPill
import com.meshlink.ui.designsystem.components.badges.MeshSecurityBadge
import com.meshlink.ui.designsystem.components.badges.MeshSignalIndicator
import com.meshlink.ui.designsystem.components.badges.MeshStatusDot
import com.meshlink.ui.designsystem.components.badges.MeshTag
import com.meshlink.ui.designsystem.components.badges.MeshTransportBadge
import com.meshlink.ui.designsystem.components.buttons.MeshDangerButton
import com.meshlink.ui.designsystem.components.buttons.MeshExtendedFAB
import com.meshlink.ui.designsystem.components.buttons.MeshFAB
import com.meshlink.ui.designsystem.components.buttons.MeshGhostButton
import com.meshlink.ui.designsystem.components.buttons.MeshIconButton
import com.meshlink.ui.designsystem.components.buttons.MeshOutlinedButton
import com.meshlink.ui.designsystem.components.buttons.MeshPrimaryButton
import com.meshlink.ui.designsystem.components.buttons.MeshSecondaryButton
import com.meshlink.ui.designsystem.components.cards.MeshInfoCard
import com.meshlink.ui.designsystem.components.cards.MeshListItem
import com.meshlink.ui.designsystem.components.cards.MeshMetricCard
import com.meshlink.ui.designsystem.components.cards.MeshQuickActionCard
import com.meshlink.ui.designsystem.components.glass.MeshGlassCard
import com.meshlink.ui.designsystem.components.glass.MeshSectionCard
import com.meshlink.ui.designsystem.components.inputs.MeshCheckbox
import com.meshlink.ui.designsystem.components.inputs.MeshLinearProgress
import com.meshlink.ui.designsystem.components.inputs.MeshPasswordField
import com.meshlink.ui.designsystem.components.inputs.MeshRadio
import com.meshlink.ui.designsystem.components.inputs.MeshSearchBar
import com.meshlink.ui.designsystem.components.inputs.MeshSegmentedControls
import com.meshlink.ui.designsystem.components.inputs.MeshSlider
import com.meshlink.ui.designsystem.components.inputs.MeshSwitch
import com.meshlink.ui.designsystem.components.inputs.MeshTextField
import com.meshlink.ui.designsystem.components.navigation.MeshNavigationBar
import com.meshlink.ui.designsystem.components.navigation.MeshNavigationBarItem
import com.meshlink.ui.designsystem.components.shared.MeshConnectionStatusPill
import com.meshlink.ui.designsystem.components.shared.MeshStatusComponent
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors

@Composable
fun DesignSystemPreviewScreen() {
    val colors = LocalMeshSemanticColors.current
    val scrollState = rememberScrollState()

    var searchQuery by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var selectedSegment by remember { mutableIntStateOf(0) }
    var switchChecked by remember { mutableStateOf(true) }
    var checkboxChecked by remember { mutableStateOf(true) }
    var radioSelected by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(0.7f) }
    var navSelected by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primaryBackground)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ──
        Text(
            text = "Mesh-Link 2026 Design System Catalog",
            style = MeshTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        // ── 1. Status Components ──
        MeshSectionCard(title = "Network Status Primitives") {
            MeshStatusComponent(
                status = "CONNECTED",
                activeNodesCount = 14,
                transportType = "Hybrid (BLE + Wi-Fi)",
                rssiDbm = -62
            )
            Spacer(modifier = Modifier.height(12.dp))
            MeshConnectionStatusPill(status = "CONNECTED", activeNodesCount = 14)
        }

        // ── 2. Glass Cards ──
        MeshSectionCard(title = "Glass Cards & Surfaces") {
            MeshGlassCard {
                Text(text = "MeshGlassCard Container", style = MeshTheme.typography.titleMedium, color = colors.textPrimary)
                Text(text = "Frosted glassmorphism background with subtle borders and press physics.", style = MeshTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            MeshMetricCard(value = "128 KB/s", label = "Mesh Bandwidth", icon = Icons.Default.Speed)
            Spacer(modifier = Modifier.height(12.dp))
            MeshQuickActionCard(title = "Broadcast SOS", subtitle = "Send emergency packet to mesh", icon = Icons.Default.Send, onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            MeshInfoCard(text = "Encrypted session active via Noise Protocol framework.")
            Spacer(modifier = Modifier.height(12.dp))
            MeshListItem(headline = "Node Alpha-7", subhead = "Distance: 1 Hop • BLE 5.3", leadingContent = { MeshAvatar(name = "Alpha") })
        }

        // ── 3. Buttons ──
        MeshSectionCard(title = "Button System") {
            MeshPrimaryButton(text = "Primary Button", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            MeshSecondaryButton(text = "Secondary Button", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            MeshOutlinedButton(text = "Outlined Button", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            MeshGhostButton(text = "Ghost Action", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            MeshDangerButton(text = "Danger Action", onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MeshIconButton(icon = Icons.Default.Add, contentDescription = "Add", onClick = {})
                Spacer(modifier = Modifier.width(12.dp))
                MeshFAB(icon = Icons.Default.Hub, contentDescription = "Mesh", onClick = {})
                Spacer(modifier = Modifier.width(12.dp))
                MeshExtendedFAB(text = "Connect Node", icon = Icons.Default.Bluetooth, onClick = {})
            }
        }

        // ── 4. Inputs ──
        MeshSectionCard(title = "Input Controls") {
            MeshSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, placeholderText = "Search mesh nodes...")
            Spacer(modifier = Modifier.height(12.dp))
            MeshTextField(value = textInput, onValueChange = { textInput = it }, label = "Peer Alias")
            Spacer(modifier = Modifier.height(12.dp))
            MeshPasswordField(value = passwordInput, onValueChange = { passwordInput = it }, label = "Network Security Key")
            Spacer(modifier = Modifier.height(12.dp))
            MeshSegmentedControls(options = listOf("All", "Peers", "Channels"), selectedIndex = selectedSegment, onOptionSelected = { selectedSegment = it })
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Mesh Switch", style = MeshTheme.typography.bodyMedium, color = colors.textPrimary)
                MeshSwitch(checked = switchChecked, onCheckedChange = { switchChecked = it })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Checkbox & Radio", style = MeshTheme.typography.bodyMedium, color = colors.textPrimary)
                Row {
                    MeshCheckbox(checked = checkboxChecked, onCheckedChange = { checkboxChecked = it })
                    Spacer(modifier = Modifier.width(16.dp))
                    MeshRadio(selected = radioSelected, onClick = { radioSelected = !radioSelected })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            MeshSlider(value = sliderValue, onValueChange = { sliderValue = it })
            Spacer(modifier = Modifier.height(8.dp))
            MeshLinearProgress(progress = sliderValue)
        }

        // ── 5. Badges & Indicators ──
        MeshSectionCard(title = "Badges & Indicators") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeshBadge(text = "M3 Expressive")
                MeshTag(text = "Production")
                MeshPill(text = "Active")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeshHopBadge(hopCount = 0)
                MeshHopBadge(hopCount = 2)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeshTransportBadge(transportName = "BLE")
                MeshTransportBadge(transportName = "Wi-Fi Direct")
            }
            Spacer(modifier = Modifier.height(12.dp))
            MeshSecurityBadge()
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Signal RSSI (-65 dBm): ", style = MeshTheme.typography.bodyMedium, color = colors.textPrimary)
                MeshSignalIndicator(rssiDbm = -65)
                Spacer(modifier = Modifier.width(16.dp))
                MeshStatusDot(color = colors.meshConnected)
                Spacer(modifier = Modifier.width(16.dp))
                MeshNotificationBadge(count = 5)
            }
        }

        // ── 6. Navigation Bar ──
        Text(text = "Navigation Bar Foundation", style = MeshTheme.typography.titleMedium, color = colors.textPrimary)
        MeshNavigationBar {
            MeshNavigationBarItem(selected = navSelected == 0, onClick = { navSelected = 0 }, icon = Icons.Default.Hub, label = "Home")
            MeshNavigationBarItem(selected = navSelected == 1, onClick = { navSelected = 1 }, icon = Icons.Default.Bluetooth, label = "Nearby")
            MeshNavigationBarItem(selected = navSelected == 2, onClick = { navSelected = 2 }, icon = Icons.Default.Send, label = "Chats")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Compose Multi-Previews ──

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

@Preview(name = "Large Font Preview", fontScale = 1.3f)
@Composable
fun DesignSystemPreviewLargeFont() {
    MeshTheme(largeTextEnabled = true) {
        DesignSystemPreviewScreen()
    }
}

@Preview(name = "Tablet Preview", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun DesignSystemPreviewTablet() {
    MeshTheme {
        DesignSystemPreviewScreen()
    }
}
