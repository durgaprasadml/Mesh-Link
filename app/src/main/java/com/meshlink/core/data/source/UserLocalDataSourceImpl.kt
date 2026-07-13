package com.meshlink.core.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meshlink.database.data.local.UserDao
import com.meshlink.database.data.local.UserEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserLocalDataSourceImpl @Inject constructor(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>
) : UserLocalDataSource {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
        val ONLINE_VISIBILITY = booleanPreferencesKey("online_visibility")
        val MESH_MODE = stringPreferencesKey("mesh_mode")
    }

    override val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
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

    override suspend fun setLoginState(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    override suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    override suspend fun getUser(meshId: String): UserEntity? {
        return userDao.getUser(meshId)
    }

    override suspend fun getLocalUser(): UserEntity? {
        return userDao.getLocalUser()
    }
}
