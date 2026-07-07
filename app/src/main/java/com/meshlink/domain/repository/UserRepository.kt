package com.meshlink.domain.repository

import com.meshlink.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun registerUser(name: String, phoneNumber: String, pin: String): Result<String>
    suspend fun loginUser(phoneNumber: String, pin: String): Result<UserEntity>
    suspend fun getLocalUser(): UserEntity?
    suspend fun logout()
    val isUserLoggedIn: Flow<Boolean>
}
