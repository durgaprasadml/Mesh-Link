package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.settings.SettingsUiState

@Composable
fun AboutSection(
    uiState: SettingsUiState,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val aboutItems = listOf(
            SettingsItemUi(
                id = "app_version",
                title = "Mesh-Link Tactical",
                subtitle = "Version 1.4.0 (Build 20260802)",
                icon = Icons.Default.Info,
                trailingText = "v1.4.0",
                onClick = { onShowToast("Mesh-Link v1.4.0 — Tactical Mesh Suite") }
            ),
            SettingsItemUi(
                id = "licenses",
                title = "Open Source Licenses",
                subtitle = "Apache 2.0, MIT & Hilt/Compose dependencies",
                icon = Icons.Default.Code,
                onClick = { onShowToast("Mesh-Link is powered by Open Source software") }
            ),
            SettingsItemUi(
                id = "privacy_policy",
                title = "Privacy Policy",
                subtitle = "Zero-cloud tracking & decentralized payload policy",
                icon = Icons.Default.Policy,
                onClick = { onShowToast("Privacy Policy: All data stays local or peer-to-peer encrypted") }
            ),
            SettingsItemUi(
                id = "terms",
                title = "Terms of Service",
                subtitle = "Off-grid emergency protocol guidelines",
                icon = Icons.Default.Description,
                onClick = { onShowToast("Terms: Emergency & Mesh protocol usage agreement") }
            ),
            SettingsItemUi(
                id = "developer_team",
                title = "Google DeepMind Advanced Engineering",
                subtitle = "Built with Antigravity AI Engine & Jetpack Compose",
                icon = Icons.Default.Star,
                onClick = { onShowToast("Mesh-Link — Off-grid resilient mesh architecture") }
            )
        )

        SettingsGroupCard(
            title = "About Mesh-Link",
            items = aboutItems
        )
    }
}
