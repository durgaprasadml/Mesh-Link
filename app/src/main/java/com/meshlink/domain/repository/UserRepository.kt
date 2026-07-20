package com.meshlink.domain.repository

import com.meshlink.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun registerUser(name: String, phoneNumber: String, pin: String): Result<String>
    suspend fun loginUser(phoneNumber: String, pin: String): Result<User>
    suspend fun getLocalUser(): User?
    suspend fun updateUserName(name: String)
    suspend fun logout()
    val isUserLoggedIn: Flow<Boolean>
    
    val isEncryptionEnabled: Flow<Boolean>
    suspend fun setEncryptionEnabled(enabled: Boolean)
    
    val isOnlineVisible: Flow<Boolean>
    suspend fun setOnlineVisible(visible: Boolean)
    
    val meshMode: Flow<String>
    suspend fun setMeshMode(mode: String)
}
