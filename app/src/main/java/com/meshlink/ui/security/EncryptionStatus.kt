package com.meshlink.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EncryptionStatusSection(
    encryptionUi: EncryptionUi,
    onKeyRotationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EncryptionStatusCard(
        encryptionUi = encryptionUi,
        onKeyRotationClick = onKeyRotationClick,
        modifier = modifier
    )
}

