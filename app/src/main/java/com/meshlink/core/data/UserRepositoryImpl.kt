package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.model.User
import com.meshlink.database.data.local.UserEntity
import com.meshlink.trust.MeshIdentityManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val identityManager: MeshIdentityManager
) : UserRepository {

    companion object {
        private val GENERIC_NAMES = setOf(
            "man", "device", "peer", "nearby node", "unknown", "unknown user", "android", "null", "-", "user"
        )

        fun isGenericOrInvalidName(name: String?, meshIdOrAddress: String? = null): Boolean {
            if (name.isNullOrBlank()) return true
            val trimmed = name.trim().lowercase()
            if (GENERIC_NAMES.contains(trimmed)) return true
            if (meshIdOrAddress != null) {
                val canonicalTarget = com.meshlink.util.MeshIdNormalizer.canonicalize(meshIdOrAddress).lowercase()
                if (trimmed == canonicalTarget || trimmed == meshIdOrAddress.trim().lowercase()) return true
            }
            return false
        }
    }

    override val hasProfile: Flow<Boolean> = localDataSource.hasProfile

    override val localUser: Flow<User?> = localDataSource.observeLocalUser().map { entity ->
        entity?.let {
            User(
                meshId = it.meshId,
                name = it.name,
                avatarUri = it.avatarUri,
                aboutMe = it.aboutMe,
                profilePhotoPath = it.profilePhotoPath,
                profilePhotoHash = it.profilePhotoHash,
                profilePhotoVersion = it.profilePhotoVersion,
                profileLastUpdated = it.profileLastUpdated
            )
        }
    }
    
    @Deprecated("Use setupProfile instead", ReplaceWith("setupProfile(name, avatarUri)"))
    override suspend fun createProfile(name: String, avatarUri: String?): Result<Unit> {
        return try {
            val identity = identityManager.getOrCreateIdentity()
            identityManager.updateDisplayName(name)
            val user = UserEntity(meshId = identity.meshId, name = name, avatarUri = avatarUri)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setupProfile(name: String, avatarUri: String?): com.meshlink.domain.model.MeshResult<Unit> {
        return try {
            val identity = identityManager.getOrCreateIdentity()
            identityManager.updateDisplayName(name)
            val user = UserEntity(meshId = identity.meshId, name = name, avatarUri = avatarUri)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            com.meshlink.common.logger.MeshLogger.i("UserRepository", "[MeshStartup] IDENTITY_READY: MeshID=${identity.meshId}, Name=$name")
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
        return userEntity?.let {
            User(
                meshId = it.meshId,
                name = it.name,
                avatarUri = it.avatarUri,
                aboutMe = it.aboutMe,
                profilePhotoPath = it.profilePhotoPath,
                profilePhotoHash = it.profilePhotoHash,
                profilePhotoVersion = it.profilePhotoVersion,
                profileLastUpdated = it.profileLastUpdated
            )
        }
    }

    override suspend fun updateUserName(name: String) {
        val userEntity = localDataSource.getLocalUser()
        if (userEntity != null) {
            localDataSource.insertUser(userEntity.copy(name = name))
            identityManager.updateDisplayName(name)
        }
    }

    override suspend fun updateProfile(name: String, aboutMe: String?, avatarUri: String?) {
        val userEntity = localDataSource.getLocalUser()
        if (userEntity != null) {
            localDataSource.insertUser(userEntity.copy(name = name, aboutMe = aboutMe, avatarUri = avatarUri))
            identityManager.updateDisplayName(name)
        }
    }

    override suspend fun getUserDisplayName(meshId: String): String {
        if (meshId.isBlank()) return "Unknown User"
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        val localUser = getLocalUser()
        if (localUser != null && com.meshlink.util.MeshIdNormalizer.canonicalize(localUser.meshId) == canonicalTargetId) {
            val localName = localUser.name.trim()
            return if (!isGenericOrInvalidName(localName, canonicalTargetId)) localName else "Unknown User"
        }
        val userEntity = localDataSource.getUser(meshId) ?: localDataSource.getUser(canonicalTargetId)
        val name = userEntity?.name?.trim()
        return if (!isGenericOrInvalidName(name, canonicalTargetId)) name!! else "Unknown User"
    }

    override suspend fun getUserProfile(meshId: String): User? {
        if (meshId.isBlank()) return null
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        val entity = localDataSource.getUser(meshId) ?: localDataSource.getUser(canonicalTargetId)
        return entity?.let {
            User(
                meshId = it.meshId,
                name = it.name,
                avatarUri = it.avatarUri,
                aboutMe = it.aboutMe,
                profilePhotoPath = it.profilePhotoPath,
                profilePhotoHash = it.profilePhotoHash,
                profilePhotoVersion = it.profilePhotoVersion,
                profileLastUpdated = it.profileLastUpdated
            )
        }
    }

    override fun observeUserProfile(meshId: String): Flow<User?> {
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        return localDataSource.observeUser(canonicalTargetId).map { entity ->
            entity?.let {
                User(
                    meshId = it.meshId,
                    name = it.name,
                    avatarUri = it.avatarUri,
                    aboutMe = it.aboutMe,
                    profilePhotoPath = it.profilePhotoPath,
                    profilePhotoHash = it.profilePhotoHash,
                    profilePhotoVersion = it.profilePhotoVersion,
                    profileLastUpdated = it.profileLastUpdated
                )
            }
        }
    }

    override suspend fun updateProfilePhoto(meshId: String, photoPath: String, photoHash: String, version: Long, lastUpdated: Long) {
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        val existingUser = localDataSource.getUser(canonicalTargetId) ?: localDataSource.getUser(meshId)
        if (existingUser != null) {
            localDataSource.updateProfilePhoto(existingUser.meshId, photoPath, photoHash, version, lastUpdated)
        } else {
            val newUser = UserEntity(
                meshId = canonicalTargetId,
                name = "User",
                profilePhotoPath = photoPath,
                profilePhotoHash = photoHash,
                profilePhotoVersion = version,
                profileLastUpdated = lastUpdated
            )
            localDataSource.insertUser(newUser)
        }
    }
}
