package com.meshlink.ui.profile

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.meshlink.domain.model.User

@Immutable
enum class TrustLevelUi(val label: String) {
    VERIFIED("Verified"),
    TRUSTED("Trusted"),
    PENDING("Pending"),
    UNKNOWN("Unknown")
}

@Immutable
data class UserIdentityUi(
    val meshId: String,
    val displayName: String,
    val aboutMe: String?,
    val avatarUri: String?,
    val isOnline: Boolean = true,
    val fingerprint: String = "",
    val completionPercentage: Int = 0,
    val trustLevel: TrustLevelUi = TrustLevelUi.VERIFIED
) {
    companion object {
        fun fromUser(user: User?, isOnline: Boolean = true): UserIdentityUi {
            val u = user ?: return UserIdentityUi(
                meshId = "LOCAL-NODE-0000",
                displayName = "Anonymous Node",
                aboutMe = null,
                avatarUri = null,
                isOnline = false,
                fingerprint = "0000 0000 0000 0000",
                completionPercentage = 30,
                trustLevel = TrustLevelUi.VERIFIED
            )

            val namePoints = if (u.name.isNotBlank()) 40 else 0
            val avatarPoints = if (!u.avatarUri.isNullOrBlank()) 30 else 0
            val aboutPoints = if (!u.aboutMe.isNullOrBlank()) 30 else 0
            val totalCompletion = (namePoints + avatarPoints + aboutPoints).coerceIn(0, 100)

            val rawFingerprint = u.meshId.take(16).padEnd(16, '0').uppercase()
            val formattedFingerprint = rawFingerprint.chunked(4).joinToString(" ")

            return UserIdentityUi(
                meshId = u.meshId,
                displayName = u.name.ifBlank { "Mesh Node" },
                aboutMe = u.aboutMe,
                avatarUri = u.avatarUri,
                isOnline = isOnline,
                fingerprint = formattedFingerprint,
                completionPercentage = totalCompletion,
                trustLevel = TrustLevelUi.VERIFIED
            )
        }
    }
}

@Immutable
data class MeshIdentityUi(
    val nodeId: String,
    val deviceName: String,
    val encryptionStatus: String,
    val activeTransport: String,
    val connectedPeersCount: Int,
    val isRelayActive: Boolean,
    val maxHops: Int,
    val ttl: Int
)

@Immutable
data class TrustedDeviceUi(
    val id: String,
    val deviceName: String,
    val meshId: String,
    val avatarUri: String? = null,
    val trustLevel: TrustLevelUi = TrustLevelUi.TRUSTED,
    val deviceModel: String = "Mesh Peer Device",
    val lastSeen: String = "Active now",
    val isOnline: Boolean = true,
    val fingerprint: String = "A1B2 C3D4 E5F6 7890"
)

@Immutable
data class ContactUi(
    val id: String,
    val displayName: String,
    val meshId: String,
    val avatarUri: String? = null,
    val isOnline: Boolean = true,
    val statusMessage: String? = "Connected via BLE Mesh",
    val lastSeen: String = "Just now",
    val isMeshConnected: Boolean = true,
    val trustLevel: TrustLevelUi = TrustLevelUi.VERIFIED,
    val fingerprint: String = "4321 8765 FEDC BA09",
    val deviceModel: String = "Android Mesh Terminal"
)

@Immutable
enum class ContactFilterOption(val label: String) {
    ALL("All"),
    NEARBY("Nearby"),
    ONLINE("Online"),
    OFFLINE("Offline"),
    TRUSTED("Trusted"),
    VERIFIED("Verified")
}

@Immutable
enum class ThemeModeOption(val key: String, val label: String) {
    LIGHT("LIGHT", "Light Mode"),
    DARK("DARK", "Dark Mode"),
    AMOLED("AMOLED", "AMOLED Black"),
    SYSTEM("SYSTEM", "System Default");

    companion object {
        fun fromKey(key: String): ThemeModeOption = entries.find { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

@Immutable
data class SettingsItemUi(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val trailingText: String? = null,
    val isChecked: Boolean? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

@Immutable
data class SettingsCategoryUi(
    val title: String,
    val items: List<SettingsItemUi>
)

@Immutable
data class DiagnosticsUi(
    val appVersion: String,
    val buildNumber: String,
    val androidVersion: String,
    val deviceModel: String,
    val meshStatus: String,
    val packetCount: Long,
    val transportLogsEnabled: Boolean
)

@Immutable
data class ConnectivityUi(
    val isBleEnabled: Boolean,
    val isBleAdvertising: Boolean,
    val isBleScanning: Boolean,
    val txPower: Int,
    val scanInterval: Long,
    val preferredTransport: String,
    val isRelayEnabled: Boolean,
    val maxHops: Int,
    val ttl: Int
)
