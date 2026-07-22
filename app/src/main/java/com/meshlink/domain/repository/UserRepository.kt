package com.meshlink.domain.repository

import com.meshlink.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun createProfile(name: String): Result<String>
    val hasProfile: Flow<Boolean>
    suspend fun getLocalUser(): User?
    suspend fun updateUserName(name: String)
    suspend fun updateProfile(name: String, aboutMe: String?, avatarUri: String?)
    
    val hasProfile: Flow<Boolean>
    suspend fun createProfile(name: String): Result<Unit>
    
    val isEncryptionEnabled: Flow<Boolean>
    suspend fun setEncryptionEnabled(enabled: Boolean)
    
    val isOnlineVisible: Flow<Boolean>
    suspend fun setOnlineVisible(visible: Boolean)
    
    val meshMode: Flow<String>
    suspend fun setMeshMode(mode: String)
}
