package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlink.ui.components.LoadingOverlay
import com.meshlink.ui.components.MeshScreen
import com.meshlink.ui.components.MeshTopAppBar
import com.meshlink.ui.designsystem.theme.MeshSpacing
import com.meshlink.ui.settings.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshProfileScreen(
    profileState: ProfileUiState,
    settingsState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onEditAvatarClick: () -> Unit,
    onSaveProfile: (name: String, aboutMe: String?, avatarUri: String?) -> Unit,
    onExportLogs: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userIdentityUi = UserIdentityUi.fromUser(
        user = profileState.user,
        isOnline = settingsState.isOnlineVisible
    )

    val meshIdentityUi = MeshIdentityUi(
        nodeId = profileState.user?.meshId?.take(12)?.uppercase() ?: "NODE-UNKNOWN",
        deviceName = android.os.Build.MODEL ?: "Android Node",
        encryptionStatus = if (settingsState.isEncryptionEnabled) "AES-256 E2EE" else "PlainText",
        activeTransport = settingsState.preferredTransport,
        connectedPeersCount = if (settingsState.isOnlineVisible) 3 else 0,
        isRelayActive = settingsState.isMeshRelayEnabled,
        maxHops = settingsState.meshMaxHops,
        ttl = settingsState.meshTtl
    )

    MeshScreen(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MeshTopAppBar(
                title = "Profile & Identity Control",
                onBackClick = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MeshSpacing.ScreenPadding),
                contentPadding = PaddingValues(top = 8.dp, bottom = MeshSpacing.ListBottomSpacing)
            ) {
                // 1. Profile Hero Section
                item {
                    ProfileHero(
                        userIdentity = userIdentityUi,
                        onEditAvatarClick = onEditAvatarClick
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 2. Identity & Cryptographic Card
                item {
                    IdentityCard(
                        userIdentity = userIdentityUi,
                        onShowQrCode = { onShowToast("QR Code: ${userIdentityUi.meshId}") }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 3. Mesh Telemetry Card
                item {
                    MeshIdentityCard(
                        meshIdentity = meshIdentityUi
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 4. Profile Details Editor Card
                item {
                    ProfileEditor(
                        currentName = userIdentityUi.displayName,
                        currentAboutMe = userIdentityUi.aboutMe,
                        currentAvatarUri = userIdentityUi.avatarUri,
                        isSaving = profileState.isSaving,
                        onSaveProfile = onSaveProfile
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 5. Diagnostics Section
                item {
                    DiagnosticsSection(
                        uiState = settingsState,
                        onExportLogs = onExportLogs,
                        onShowToast = onShowToast
                    )
                }
            }

            LoadingOverlay(isLoading = profileState.isLoading)
        }
    }
}
