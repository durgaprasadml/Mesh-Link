package com.meshlink.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meshlink.domain.model.User
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun ProfileHeroLightPreview() {
    MeshTheme(themeMode = "LIGHT") {
        Surface {
            ProfileHero(
                userIdentity = UserIdentityUi.fromUser(
                    User(
                        meshId = "NODE-8A9F-B4C2",
                        name = "Commander Alpha",
                        aboutMe = "Tactical communications lead for sector 4.",
                        avatarUri = null
                    )
                ),
                onEditAvatarClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun IdentityCardDarkPreview() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                IdentityCard(
                    userIdentity = UserIdentityUi.fromUser(
                        User(
                            meshId = "NODE-8A9F-B4C2",
                            name = "Commander Alpha",
                            aboutMe = "Tactical communications lead.",
                            avatarUri = null
                        )
                    )
                )
            }
        }
    }
}

@Preview(name = "AMOLED Preview", showBackground = true)
@Composable
fun ThemeSelectorPreview() {
    MeshTheme(themeMode = "AMOLED", amoledDark = true) {
        Surface {
            ThemeSelector(
                currentTheme = "AMOLED",
                onSelectTheme = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Tablet Layout", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun MeshSettingsScreenTabletPreview() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            MeshSettingsScreen(
                uiState = SettingsUiState(),
                onBack = {},
                onSetThemeMode = {},
                onSetMaterialYou = {},
                onSetHighContrast = {},
                onSetGlassEffects = {},
                onSetReduceMotion = {},
                onSetLargeText = {},
                onSetEncryptionEnabled = {},
                onSetOnlineVisible = {},
                onSetAdvancedEncryption = {},
                onSetBleEnabled = {},
                onSetBleAdv = {},
                onSetBleScan = {},
                onSetTransport = {},
                onSetRelayEnabled = {},
                onSetMaxHops = {},
                onExportLogs = {},
                onShowToast = {}
            )
        }
    }
}
