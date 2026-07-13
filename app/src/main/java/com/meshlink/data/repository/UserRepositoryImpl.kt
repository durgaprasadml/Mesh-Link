package com.meshlink.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.meshlink.data.local.UserDao
import com.meshlink.data.local.UserEntity
import com.meshlink.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>
) : UserRepository {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
        val ONLINE_VISIBILITY = booleanPreferencesKey("online_visibility")
        val MESH_MODE = stringPreferencesKey("mesh_mode")
    }

    override val isUserLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }
    
    override val isEncryptionEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ENCRYPTION_ENABLED] ?: true
    }
    
    override suspend fun setEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[ENCRYPTION_ENABLED] = enabled }
    }
    
    override val isOnlineVisible: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONLINE_VISIBILITY] ?: true
    }
    
    override suspend fun setOnlineVisible(visible: Boolean) {
        dataStore.edit { prefs -> prefs[ONLINE_VISIBILITY] = visible }
    }
    
    override val meshMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[MESH_MODE] ?: "Auto"
    }
    
    override suspend fun setMeshMode(mode: String) {
        dataStore.edit { prefs -> prefs[MESH_MODE] = mode }
    }

    override suspend fun registerUser(name: String, phoneNumber: String, pin: String): Result<String> {
        return try {
            val meshId = generateMeshId(phoneNumber)
            val salt = generateSalt()
            val pinHash = "$salt:${generateSaltedHash(pin, salt)}"
            val user = UserEntity(meshId = meshId, name = name, phoneNumber = phoneNumber, pinHash = pinHash)
            userDao.insertUser(user)
            setLoginState(true)
            Result.success(meshId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(phoneNumber: String, pin: String): Result<UserEntity> {
        return try {
            val meshId = generateMeshId(phoneNumber)
            val user = userDao.getUser(meshId)
            
            if (user != null) {
                var verified = false
                
                if (user.pinHash.contains(":")) {
                    val parts = user.pinHash.split(":")
                    if (parts.size == 2) {
                        val salt = parts[0]
                        val hash = parts[1]
                        if (generateSaltedHash(pin, salt) == hash) {
                            verified = true
                        }
                    }
                } else {
                    // Legacy migration
                    val legacyHash = generateLegacyHash(pin)
                    if (user.pinHash == legacyHash) {
                        verified = true
                        val newSalt = generateSalt()
                        val newHash = generateSaltedHash(pin, newSalt)
                        userDao.insertUser(user.copy(pinHash = "$newSalt:$newHash"))
                    }
                }
                
                if (verified) {
                    setLoginState(true)
                    return Result.success(user)
                }
            }
            
            Result.failure(Exception("Invalid phone number or PIN"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalUser(): UserEntity? {
        return userDao.getLocalUser()
    }

    override suspend fun logout() {
        setLoginState(false)
    }

    private suspend fun setLoginState(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    private fun generateMeshId(phoneNumber: String): String {
        return generateLegacyHash(phoneNumber)
    }

    private fun generateLegacyHash(input: String): String {
        val bytes = input.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun generateSalt(): String {
        val random = java.security.SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
    }

    private fun generateSaltedHash(input: String, salt: String): String {
        val iterations = 10000
        val keyLength = 256
        val spec = javax.crypto.spec.PBEKeySpec(
            input.toCharArray(),
            android.util.Base64.decode(salt, android.util.Base64.NO_WRAP),
            iterations,
            keyLength
        )
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = skf.generateSecret(spec).encoded
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
}
