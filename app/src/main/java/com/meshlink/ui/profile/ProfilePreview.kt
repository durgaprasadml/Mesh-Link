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
import com.meshlink.ui.contacts.ContactsList
import com.meshlink.ui.designsystem.theme.MeshTheme
import com.meshlink.ui.settings.SettingsUiState

@Preview(name = "Light Mode Phone", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun ProfileHeroLightPreview() {
    MeshTheme(themeMode = "LIGHT") {
        Surface {
            ProfileHero(
                userIdentity = UserIdentityUi.fromUser(
                    User(
                        meshId = "NODE-8A9F-B4C2",
                        name = "Commander Alpha",
                        aboutMe = "Tactical mesh node lead for Sector 4.",
                        avatarUri = null
                    )
                ),
                onEditAvatarClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Dark Mode Identity Card", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
                            aboutMe = "Cryptographic Mesh Node",
                            avatarUri = null
                        )
                    )
                )
            }
        }
    }
}

@Preview(name = "AMOLED Black Trusted Devices", showBackground = true)
@Composable
fun TrustedDevicesAmoledPreview() {
    MeshTheme(themeMode = "AMOLED", amoledDark = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                TrustedDevices()
            }
        }
    }
}

@Preview(name = "Material You Dynamic Colors", showBackground = true)
@Composable
fun VerificationCardMaterialYouPreview() {
    MeshTheme(themeMode = "LIGHT", dynamicColor = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                VerificationCard()
            }
        }
    }
}

@Preview(name = "Contacts List Preview", showBackground = true)
@Composable
fun ContactsListPreview() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            ContactsList()
        }
    }
}

@Preview(name = "Tablet Split Layout", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun MeshProfileScreenTabletPreview() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            MeshProfileScreen(
                profileState = ProfileUiState(),
                settingsState = SettingsUiState(),
                onNavigateBack = {},
                onEditAvatarClick = {},
                onSaveProfile = { _, _, _ -> },
                onExportLogs = {},
                onShowToast = {}
            )
        }
    }
}

@Preview(name = "Foldable Layout Preview", device = "spec:width=673dp,height=841dp,dpi=300")
@Composable
fun MeshProfileScreenFoldablePreview() {
    MeshTheme(themeMode = "LIGHT") {
        Surface {
            MeshProfileScreen(
                profileState = ProfileUiState(),
                settingsState = SettingsUiState(),
                onNavigateBack = {},
                onEditAvatarClick = {},
                onSaveProfile = { _, _, _ -> },
                onExportLogs = {},
                onShowToast = {}
            )
        }
    }
}

@Preview(name = "Landscape Phone Preview", device = "spec:width=891dp,height=411dp,dpi=420")
@Composable
fun MeshProfileScreenLandscapePreview() {
    MeshTheme(themeMode = "DARK") {
        Surface {
            MeshProfileScreen(
                profileState = ProfileUiState(),
                settingsState = SettingsUiState(),
                onNavigateBack = {},
                onEditAvatarClick = {},
                onSaveProfile = { _, _, _ -> },
                onExportLogs = {},
                onShowToast = {}
            )
        }
    }
}

@Preview(name = "Large Font Accessibility Preview", fontScale = 1.5f, showBackground = true)
@Composable
fun ProfileQuickActionsLargeFontPreview() {
    MeshTheme(themeMode = "LIGHT") {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileQuickActions(
                    onEditProfileClick = {},
                    onQrCodeClick = {},
                    onTrustedDevicesClick = {},
                    onContactsClick = {}
                )
            }
        }
    }
}
