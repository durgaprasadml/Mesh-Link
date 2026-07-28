package com.meshlink.core.data.source

import com.meshlink.database.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserLocalDataSource {
    val hasProfile: Flow<Boolean>
    val isEncryptionEnabled: Flow<Boolean>
    val isOnlineVisible: Flow<Boolean>
    val meshMode: Flow<String>

    suspend fun setProfileCreated(created: Boolean)
    suspend fun setEncryptionEnabled(enabled: Boolean)
    suspend fun setOnlineVisible(visible: Boolean)
    suspend fun setMeshMode(mode: String)

    suspend fun insertUser(user: UserEntity)
    suspend fun getUser(meshId: String): UserEntity?
    suspend fun getLocalUser(): UserEntity?
    suspend fun clearLocalData()
}
