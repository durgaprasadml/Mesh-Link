package com.meshlink.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.profile.UserIdentityUi

@Preview(name = "Phone Light", showBackground = true, device = Devices.PIXEL_7)
@Composable
fun SettingsPhoneLightPreview() {
    MeshTheme(themeMode = "LIGHT") {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsProfileCard(
                userIdentity = UserIdentityUi(
                    meshId = "8F42E9A1NODE0001",
                    displayName = "Tactical Node 1",
                    aboutMe = "Mesh Operator",
                    avatarUri = null,
                    isOnline = true
                )
            )
        }
    }
}

@Preview(name = "Phone Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7)
@Composable
fun SettingsPhoneDarkPreview() {
    MeshTheme(themeMode = "DARK") {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsProfileCard(
                userIdentity = UserIdentityUi(
                    meshId = "8F42E9A1NODE0001",
                    displayName = "Tactical Node 1",
                    aboutMe = "Mesh Operator",
                    avatarUri = null,
                    isOnline = true
                )
            )
        }
    }
}

@Preview(name = "Tablet Two Pane", showBackground = true, device = Devices.PIXEL_TABLET)
@Composable
fun SettingsTabletPreview() {
    MeshTheme(themeMode = "SYSTEM") {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            QuickPreferences(
                onNavigateToAppearance = {},
                onNavigateToPrivacy = {},
                onNavigateToNotifications = {},
                onNavigateToConnectivity = {}
            )
        }
    }
}
