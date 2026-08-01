package com.meshlink.ui.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.designsystem.components.badges.AvatarGroup
import com.meshlink.ui.designsystem.components.badges.MeshAvatar
import com.meshlink.ui.designsystem.components.badges.MeshBadge
import com.meshlink.ui.designsystem.components.badges.MeshChip
import com.meshlink.ui.designsystem.components.buttons.MeshButton
import com.meshlink.ui.designsystem.components.buttons.MeshButtonVariant
import com.meshlink.ui.designsystem.components.buttons.MeshSegmentedControl
import com.meshlink.ui.designsystem.components.cards.HeroCard
import com.meshlink.ui.designsystem.components.cards.MeshCard
import com.meshlink.ui.designsystem.components.cards.MetricCard
import com.meshlink.ui.designsystem.components.glass.MeshGlassCard
import com.meshlink.ui.designsystem.components.inputs.MeshInputField
import com.meshlink.ui.designsystem.components.inputs.MeshSearchBar
import com.meshlink.ui.designsystem.components.shared.HopBadge
import com.meshlink.ui.designsystem.components.shared.MeshStatusBadge
import com.meshlink.ui.designsystem.components.shared.ProgressBar
import com.meshlink.ui.designsystem.components.shared.ProgressRing
import com.meshlink.ui.designsystem.components.shared.SignalMeter
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Interactive Design System Catalog showcasing all 40+ components across Light, Dark, and AMOLED modes.
 */
@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeshTheme.colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "MESH-LINK DESIGN SYSTEM CATALOG 2026",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MeshTheme.colors.primary
        )

        // Buttons Section
        Text(text = "BUTTONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeshButton(text = "Primary", onClick = {})
            MeshButton(text = "Emergency", onClick = {}, variant = MeshButtonVariant.EMERGENCY)
        }

        // Telemetry Section
        Text(text = "TELEMETRY & STATUS METERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SignalMeter(rssiDbm = -62)
            HopBadge(hopCount = 1)
            MeshStatusBadge(status = "MESH ACTIVE")
        }

        // Cards & Glass Section
        Text(text = "TACTILE CARDS & GLASS SURFACES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textSecondary)
        HeroCard(
            title = "Aether Mesh Protocol",
            subtitle = "Active peer topology connected via 128-bit AES encrypted channel."
        )

        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Luminous 0.5dp Keyline Glass Container", color = MeshTheme.colors.textPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(label = "RSSI", value = "-58", unit = "dBm")
            MetricCard(label = "Active Peers", value = "14", unit = "Nodes")
        }

        // Inputs Section
        Text(text = "INPUTS & SEARCH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textSecondary)
        MeshSearchBar(query = "", onQueryChange = {})

        // Avatars & Badges
        Text(text = "AVATARS & BADGES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MeshTheme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeshAvatar(name = "Alpha Leader", isOnline = true)
            AvatarGroup(names = listOf("Alpha", "Bravo", "Charlie", "Delta"))
            MeshChip(label = "BLE MESH", selected = true, onClick = {})
        }
    }
}

@Preview(name = "Catalog Light Mode")
@Composable
private fun CatalogPreviewLight() {
    MeshTheme(themeMode = "LIGHT") {
        DesignSystemCatalog()
    }
}

@Preview(name = "Catalog Dark Mode")
@Composable
private fun CatalogPreviewDark() {
    MeshTheme(themeMode = "DARK") {
        DesignSystemCatalog()
    }
}

@Preview(name = "Catalog AMOLED Mode")
@Composable
private fun CatalogPreviewAmoled() {
    MeshTheme(themeMode = "DARK", amoledDark = true) {
        DesignSystemCatalog()
    }
}
