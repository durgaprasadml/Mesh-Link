package com.meshlink.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    }

    override val isUserLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    override suspend fun registerUser(name: String, phoneNumber: String, pin: String): Result<String> {
        return try {
            val meshId = generateMeshId(phoneNumber)
            val pinHash = generateHash(pin)
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
            
            if (user != null && user.pinHash == generateHash(pin)) {
                setLoginState(true)
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid phone number or PIN"))
            }
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
        return generateHash(phoneNumber)
    }

    private fun generateHash(input: String): String {
        val bytes = input.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
