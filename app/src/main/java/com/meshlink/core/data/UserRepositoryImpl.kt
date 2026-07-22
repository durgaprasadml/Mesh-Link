package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.model.User
import com.meshlink.database.data.local.UserEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource
) : UserRepository {

    override val hasProfile: Flow<Boolean> = localDataSource.hasProfile
    
    override suspend fun createProfile(name: String): Result<Unit> {
        return try {
            // Generate a random meshId and save local user
            val meshId = java.util.UUID.randomUUID().toString()
            val user = UserEntity(meshId = meshId, name = name)
            localDataSource.insertUser(user)
            localDataSource.setProfileCreated(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
