package com.meshlink.ui.settings.screens

import androidx.compose.runtime.Composable
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.MeshSecurityMonitor
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.ui.security.SecurityDiagnosticsBridge

@Composable
fun SecurityDiagnosticsScreen(
    cryptoManager: MeshCryptoManager,
    sessionManager: SessionManager,
    rekeyManager: RekeyManager,
    securityMonitor: MeshSecurityMonitor,
    onBack: () -> Unit
) {
    SecurityDiagnosticsBridge(
        cryptoManager = cryptoManager,
        sessionManager = sessionManager,
        rekeyManager = rekeyManager,
        securityMonitor = securityMonitor,
        onBack = onBack
    )
}
