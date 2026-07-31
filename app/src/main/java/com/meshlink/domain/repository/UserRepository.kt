package com.meshlink.domain.repository

import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Manages local user profile and application settings.
 *
 * Responsibility: Provide access to the local user data and preferences.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe.
 */
interface UserRepository {

    suspend fun getLocalUser(): User?
    val localUser: Flow<User?>
    suspend fun updateUserName(name: String)
    suspend fun updateProfile(name: String, aboutMe: String?, avatarUri: String?)
    
    val hasProfile: Flow<Boolean>

    @Deprecated("Use setupProfile instead", ReplaceWith("setupProfile(name, avatarUri)"))
    suspend fun createProfile(name: String, avatarUri: String? = null): Result<Unit>

    /**
     * Creates a new local user profile.
     *
     * @param name The user's name.
     * @param avatarUri The optional profile image URI or avatar string.
     * @return [MeshResult.Success] on success, [MeshResult.Error] on IO failure.
     */
    suspend fun setupProfile(name: String, avatarUri: String? = null): MeshResult<Unit>
    
    val isEncryptionEnabled: Flow<Boolean>
    suspend fun setEncryptionEnabled(enabled: Boolean)
    
    val isOnlineVisible: Flow<Boolean>
    suspend fun setOnlineVisible(visible: Boolean)
    
    val meshMode: Flow<String>
    suspend fun setMeshMode(mode: String)

    val localIdentity: Flow<com.meshlink.domain.model.UserIdentity?>
    val peerIdentities: kotlinx.coroutines.flow.StateFlow<Map<String, com.meshlink.domain.model.UserIdentity>>
    fun observeIdentity(userId: String, fallbackDisplayName: String? = null, fallbackAvatarUri: String? = null): Flow<com.meshlink.domain.model.UserIdentity>
    fun updatePeerIdentity(userId: String, displayName: String?, avatarUri: String?, lastUpdated: Long = System.currentTimeMillis())
}

