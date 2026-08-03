package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.model.User
import com.meshlink.database.data.local.UserEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource
) : UserRepository {

    override val hasProfile: Flow<Boolean> = localDataSource.hasProfile

    override val localUser: Flow<User?> = localDataSource.observeLocalUser().map { entity ->
        entity?.let { User(meshId = it.meshId, name = it.name, avatarUri = it.avatarUri, aboutMe = it.aboutMe) }
    }
    
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

    override suspend fun getUserDisplayName(meshId: String): String {
        val canonicalTargetId = com.meshlink.util.MeshIdNormalizer.canonicalize(meshId)
        val localUser = getLocalUser()
        if (localUser != null && com.meshlink.util.MeshIdNormalizer.canonicalize(localUser.meshId) == canonicalTargetId) {
            return localUser.name.trim().ifBlank { "Unknown User" }
        }
        val userEntity = localDataSource.getUser(meshId) ?: localDataSource.getUser(canonicalTargetId)
        val name = userEntity?.name?.trim()
        return if (!name.isNullOrBlank()) name else "Unknown User"
    }
}
