package com.meshlink.ui.profile

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.settings.SettingsUiState

@Composable
fun DiagnosticsSection(
    uiState: SettingsUiState,
    onExportLogs: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val diagnosticsItems = listOf(
            SettingsItemUi(
                id = "dev_mode",
                title = "Developer Diagnostics Mode",
                subtitle = "Enable raw telemetry graph and packet inspector",
                icon = Icons.Default.DeveloperMode,
                isChecked = uiState.developerMode,
                onClick = { onShowToast("Developer mode toggled") }
            ),
            SettingsItemUi(
                id = "packet_count",
                title = "Total Routed Packets",
                subtitle = "Lifetime mesh frame telemetry count",
                icon = Icons.Default.Memory,
                trailingText = "${uiState.packetCount}",
                onClick = { onShowToast("Total routed packet count: ${uiState.packetCount}") }
            ),
            SettingsItemUi(
                id = "device_info",
                title = "Host Device Spec",
                subtitle = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                icon = Icons.Default.Smartphone,
                onClick = {}
            ),
            SettingsItemUi(
                id = "build_spec",
                title = "Mesh Protocol Spec",
                subtitle = "Build v1.4.0 (Mesh Protocol Rev 3.2)",
                icon = Icons.Default.Code,
                onClick = {}
            ),
            SettingsItemUi(
                id = "export_logs",
                title = "Export Diagnostic Logs",
                subtitle = "Save encrypted transport logs to external storage",
                icon = Icons.Default.FileDownload,
                onClick = onExportLogs
            )
        )

        SettingsGroupCard(
            title = "Diagnostics & System Telemetry",
            items = diagnosticsItems
        )
    }
}
