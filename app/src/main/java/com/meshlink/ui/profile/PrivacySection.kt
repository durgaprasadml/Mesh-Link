package com.meshlink.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meshlink.ui.settings.SettingsUiState

@Composable
fun PrivacySection(
    uiState: SettingsUiState,
    onSetEncryptionEnabled: (Boolean) -> Unit,
    onSetOnlineVisible: (Boolean) -> Unit,
    onSetAdvancedEncryption: (Boolean) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val privacyItems = listOf(
            SettingsItemUi(
                id = "e2ee",
                title = "End-to-End Encryption (AES-256)",
                subtitle = "Encrypt all payload packets using peer ECDH keys",
                icon = Icons.Default.Lock,
                isChecked = uiState.isEncryptionEnabled,
                onClick = { onSetEncryptionEnabled(!uiState.isEncryptionEnabled) }
            ),
            SettingsItemUi(
                id = "adv_e2ee",
                title = "Strict Cryptographic Enforcement",
                subtitle = "Reject unencrypted fallback packets from untrusted nodes",
                icon = Icons.Default.EnhancedEncryption,
                isChecked = uiState.advancedEncryptionEnforcement,
                onClick = { onSetAdvancedEncryption(!uiState.advancedEncryptionEnforcement) }
            ),
            SettingsItemUi(
                id = "online_vis",
                title = "Mesh Node Visibility",
                subtitle = "Broadcast active status to nearby mesh peers",
                icon = Icons.Default.Visibility,
                isChecked = uiState.isOnlineVisible,
                onClick = { onSetOnlineVisible(!uiState.isOnlineVisible) }
            ),
            SettingsItemUi(
                id = "biometric",
                title = "Biometric Lock",
                subtitle = "Require fingerprint/face auth to view local messages",
                icon = Icons.Default.Fingerprint,
                isChecked = uiState.biometricUnlock,
                onClick = { onShowToast("Biometric authentication toggled") }
            ),
            SettingsItemUi(
                id = "trusted_keys",
                title = "Trusted Identity Keys",
                subtitle = "${uiState.trustedDevicesCount} verified peer key fingerprints",
                icon = Icons.Default.Security,
                trailingText = "${uiState.trustedDevicesCount} Keys",
                onClick = { onShowToast("Managing ${uiState.trustedDevicesCount} trusted public keys") }
            )
        )

        SettingsGroupCard(
            title = "Privacy & Security Preferences",
            items = privacyItems
        )
    }
}
