package com.meshlink.ui.sos

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.model.TransportType
import com.meshlink.ui.designsystem.theme.MeshTheme

private val sampleState = SosUiState(
    status = SosStatus.SAFE,
    isFetchingLocation = false,
    latitude = 37.77492,
    longitude = -122.41942,
    batteryPercent = 88,
    address = "Market St, San Francisco, CA",
    isBleEnabled = true,
    isWifiDirectEnabled = true,
    meshHealth = "Optimal",
    nearbyResponders = listOf(
        BleDevice(meshId = "mesh-1", name = "Alpha Node", address = "AA:BB:CC:11:22:33", rssi = -60, transport = TransportType.BLE),
        BleDevice(meshId = "mesh-2", name = "Bravo P2P", address = "DD:EE:FF:44:55:66", rssi = -45, transport = TransportType.WIFI_DIRECT)
    ),
    relaysReached = 3,
    isFlashlightOn = false,
    isAlarmPlaying = false
)

private val broadcastingState = sampleState.copy(
    status = SosStatus.BROADCASTING,
    isSending = true,
    relaysReached = 4
)

@Preview(name = "Light Mode - Standby", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewEmergencyScreenLight() {
    MeshTheme(themeMode = "LIGHT") {
        Surface {
            MeshEmergencyScreen(
                state = sampleState,
                onBack = {},
                onSendSos = {},
                onResetSos = {},
                onRefreshLocation = {},
                onToggleFlashlight = {},
                onToggleAlarm = {},
                onDismissError = {}
            )
        }
    }
}

@Preview(name = "Dark Mode - Broadcasting", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEmergencyScreenDark() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            MeshEmergencyScreen(
                state = broadcastingState,
                onBack = {},
                onSendSos = {},
                onResetSos = {},
                onRefreshLocation = {},
                onToggleFlashlight = {},
                onToggleAlarm = {},
                onDismissError = {}
            )
        }
    }
}

@Preview(name = "AMOLED High Contrast", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEmergencyScreenAmoled() {
    MeshTheme(themeMode = "DARK", amoledDark = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MeshEmergencyScreen(
                state = sampleState,
                onBack = {},
                onSendSos = {},
                onResetSos = {},
                onRefreshLocation = {},
                onToggleFlashlight = {},
                onToggleAlarm = {},
                onDismissError = {}
            )
        }
    }
}

@Preview(name = "Tablet Layout", device = Devices.TABLET, showBackground = true)
@Composable
fun PreviewEmergencyScreenTablet() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            MeshEmergencyScreen(
                state = sampleState,
                onBack = {},
                onSendSos = {},
                onResetSos = {},
                onRefreshLocation = {},
                onToggleFlashlight = {},
                onToggleAlarm = {},
                onDismissError = {}
            )
        }
    }
}

@Preview(name = "Foldable Layout", device = Devices.FOLDABLE, showBackground = true)
@Composable
fun PreviewEmergencyScreenFoldable() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            MeshEmergencyScreen(
                state = sampleState,
                onBack = {},
                onSendSos = {},
                onResetSos = {},
                onRefreshLocation = {},
                onToggleFlashlight = {},
                onToggleAlarm = {},
                onDismissError = {}
            )
        }
    }
}
