package com.meshlink.core.data

import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.repository.UserRepository
import com.meshlink.domain.model.User
import com.meshlink.security.data.source.CryptoDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val cryptoDataSource: CryptoDataSource
) : UserRepository {

    override val isUserLoggedIn: Flow<Boolean> = localDataSource.isLoggedIn
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

    override suspend fun registerUser(name: String, phoneNumber: String, pin: String): Result<String> {
        return try {
            val meshId = cryptoDataSource.generateLegacyHash(phoneNumber)
            val salt = cryptoDataSource.generateSalt()
            val pinHash = "$salt:${cryptoDataSource.generateSaltedHash(pin, salt)}"
            val user = UserEntity(meshId = meshId, name = name, phoneNumber = phoneNumber, pinHash = pinHash)
            
            localDataSource.insertUser(user)
            localDataSource.setLoginState(true)
            
            Result.success(meshId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(phoneNumber: String, pin: String): Result<User> {
        return try {
            val meshId = cryptoDataSource.generateLegacyHash(phoneNumber)
            val user = localDataSource.getUser(meshId)
            
            if (user != null) {
                var verified = false
                
                if (user.pinHash.contains(":")) {
                    val parts = user.pinHash.split(":")
                    if (parts.size == 2) {
                        val salt = parts[0]
                        val hash = parts[1]
                        if (cryptoDataSource.generateSaltedHash(pin, salt) == hash) {
                            verified = true
                        }
                    }
                } else {
                    // Legacy migration
                    val legacyHash = cryptoDataSource.generateLegacyHash(pin)
                    if (user.pinHash == legacyHash) {
                        verified = true
                        val newSalt = cryptoDataSource.generateSalt()
                        val newHash = cryptoDataSource.generateSaltedHash(pin, newSalt)
                        localDataSource.insertUser(user.copy(pinHash = "$newSalt:$newHash"))
                    }
                }
                
                if (verified) {
                    localDataSource.setLoginState(true)
                    return Result.success(com.meshlink.domain.model.User(meshId = user.meshId, name = user.name, phoneNumber = user.phoneNumber))
                }
            }
            
            Result.failure(Exception("Invalid phone number or PIN"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalUser(): com.meshlink.domain.model.User? {
        val userEntity = localDataSource.getLocalUser()
        return userEntity?.let { com.meshlink.domain.model.User(meshId = it.meshId, name = it.name, phoneNumber = it.phoneNumber) }
    }

    override suspend fun logout() {
        localDataSource.setLoginState(false)
    }
}
