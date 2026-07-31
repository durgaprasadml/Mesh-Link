package com.meshlink.domain.model

import androidx.compose.runtime.Immutable
import com.meshlink.ui.profile.AvatarAssets

enum class AvatarType {
    GALLERY,
    CAMERA,
    PRESET_AVATAR,
    INITIALS,
    UNKNOWN
}

@Immutable
data class UserIdentity(
    val userId: String,
    val displayName: String,
    val avatarType: AvatarType = AvatarType.INITIALS,
    val avatarResource: Int? = null,
    val galleryImageUri: String? = null,
    val cameraImageUri: String? = null,
    val selectedAvatarId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val initial: String?
        get() = displayName.trim().firstOrNull()?.uppercaseChar()?.toString()

    companion object {
        fun create(
            userId: String,
            displayName: String,
            avatarUri: String? = null,
            lastUpdated: Long = System.currentTimeMillis()
        ): UserIdentity {
            val cleanName = displayName.ifBlank { "User" }
            if (avatarUri.isNullOrBlank()) {
                return UserIdentity(
                    userId = userId,
                    displayName = cleanName,
                    avatarType = if (cleanName.isNotBlank()) AvatarType.INITIALS else AvatarType.UNKNOWN,
                    lastUpdated = lastUpdated
                )
            }

            return when {
                AvatarAssets.isAvatarUri(avatarUri) -> {
                    val resId = AvatarAssets.getAvatarResId(avatarUri)
                    val cleanAvatarId = when {
                        avatarUri.startsWith("avatar://") -> avatarUri.removePrefix("avatar://")
                        avatarUri.startsWith("android.resource://") -> avatarUri.substringAfterLast("/")
                        else -> avatarUri
                    }
                    UserIdentity(
                        userId = userId,
                        displayName = cleanName,
                        avatarType = AvatarType.PRESET_AVATAR,
                        avatarResource = resId,
                        selectedAvatarId = cleanAvatarId,
                        lastUpdated = lastUpdated
                    )
                }
                avatarUri.contains("camera", ignoreCase = true) || avatarUri.contains("profile_camera_", ignoreCase = true) -> {
                    UserIdentity(
                        userId = userId,
                        displayName = cleanName,
                        avatarType = AvatarType.CAMERA,
                        cameraImageUri = avatarUri,
                        lastUpdated = lastUpdated
                    )
                }
                else -> {
                    UserIdentity(
                        userId = userId,
                        displayName = cleanName,
                        avatarType = AvatarType.GALLERY,
                        galleryImageUri = avatarUri,
                        lastUpdated = lastUpdated
                    )
                }
            }
        }
    }
}
