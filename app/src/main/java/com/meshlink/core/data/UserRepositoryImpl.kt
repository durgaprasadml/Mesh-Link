package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.model.User
import com.meshlink.database.data.local.UserEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.meshlink.domain.model.UserIdentity

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource
) : UserRepository {

    private val _peerIdentities = MutableStateFlow<Map<String, UserIdentity>>(emptyMap())
    override val peerIdentities: StateFlow<Map<String, UserIdentity>> = _peerIdentities.asStateFlow()

    override val localUser: Flow<User?> = localDataSource.observeLocalUser().map { entity ->
        entity?.let { User(meshId = it.meshId, name = it.name, avatarUri = it.avatarUri, aboutMe = it.aboutMe) }
    }

    override val localIdentity: Flow<UserIdentity?> = localUser.map { user ->
        user?.let { UserIdentity.create(userId = it.meshId, displayName = it.name, avatarUri = it.avatarUri) }
    }

    override fun observeIdentity(
        userId: String,
        fallbackDisplayName: String?,
        fallbackAvatarUri: String?
    ): Flow<UserIdentity> {
        return combine(localIdentity, peerIdentities) { local, peers ->
            if (local != null && local.userId == userId) {
                local
            } else if (peers.containsKey(userId)) {
                peers.getValue(userId)
            } else {
                UserIdentity.create(
                    userId = userId,
                    displayName = fallbackDisplayName ?: userId,
                    avatarUri = fallbackAvatarUri
                )
            }
        }
    }

    override fun updatePeerIdentity(
        userId: String,
        displayName: String?,
        avatarUri: String?,
        lastUpdated: Long
    ) {
        if (userId.isBlank()) return
        val currentMap = _peerIdentities.value
        val existing = currentMap[userId]
        if (existing == null || lastUpdated >= existing.lastUpdated) {
            val updatedIdentity = UserIdentity.create(
                userId = userId,
                displayName = displayName ?: existing?.displayName ?: userId,
                avatarUri = avatarUri ?: existing?.selectedAvatarId ?: existing?.galleryImageUri ?: existing?.cameraImageUri,
                lastUpdated = lastUpdated
            )
            _peerIdentities.value = currentMap + (userId to updatedIdentity)
        }
    }

    override val hasProfile: Flow<Boolean> = localDataSource.hasProfile

    @Deprecated("Use setupProfile instead", ReplaceWith("setupProfile(name, avatarUri)"))
    override suspend fun createProfile(name: String, avatarUri: String?): Result<Unit> {
        return try {
            val meshId = java.util.UUID.randomUUID().toString()
            val user = UserEntity(meshId = meshId, name = name, avatarUri = avatarUri)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setupProfile(name: String, avatarUri: String?): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            val meshId = java.util.UUID.randomUUID().toString()
            val user = UserEntity(meshId = meshId, name = name, avatarUri = avatarUri)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            com.meshlink.domain.model.MeshResult.Success(Unit)
        } catch (e: Exception) {
            com.meshlink.domain.model.MeshResult.Error(com.meshlink.domain.model.MeshError.UnknownError("Failed to setup profile", e))
        }
    }

    override val isEncryptionEnabled: Flow<Boolean> = localDataSource.isEncryptionEnabled
    
    override suspend fun setEncryptionEnabled(enabled: Boolean) {
        localDataSource.setEncryptionEnabled(enabled)
    }
    
    override val isOnlineVisible: Flow<Boolean> = localDataSource.isOnlineVisible
    
    override suspend fun setOnlineVisible(visible: Boolean) {
        localDataSource.setOnlineVisible(visible)
    }
    
    override val meshMode: Flow<String> = localDataSource.meshMode
    
    override suspend fun setMeshMode(mode: String) {
        localDataSource.setMeshMode(mode)
    }

    override suspend fun getLocalUser(): User? {
        val userEntity = localDataSource.getLocalUser()
        return userEntity?.let { User(meshId = it.meshId, name = it.name, avatarUri = it.avatarUri, aboutMe = it.aboutMe) }
    }

    override suspend fun updateUserName(name: String) {
        val userEntity = localDataSource.getLocalUser()
        if (userEntity != null) {
            localDataSource.insertUser(userEntity.copy(name = name))
        }
    }

    override suspend fun updateProfile(name: String, aboutMe: String?, avatarUri: String?) {
        val userEntity = localDataSource.getLocalUser()
        if (userEntity != null) {
            localDataSource.insertUser(userEntity.copy(name = name, aboutMe = aboutMe, avatarUri = avatarUri))
        }
    }
}

