package com.meshlink.ui.profile

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.meshlink.domain.model.User

@Immutable
data class UserIdentityUi(
    val meshId: String,
    val displayName: String,
    val aboutMe: String?,
    val avatarUri: String?,
    val isOnline: Boolean = true,
    val fingerprint: String = "",
    val completionPercentage: Int = 0
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
                completionPercentage = 30
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
                completionPercentage = totalCompletion
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
