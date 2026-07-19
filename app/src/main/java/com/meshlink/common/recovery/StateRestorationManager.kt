package com.meshlink.common.recovery

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.meshlink.common.logger.MeshLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.recoveryDataStore by preferencesDataStore(name = "meshlink_recovery_state")

@Serializable
data class RestorationState(
    val lastScreen: String? = null,
    val activeChatId: String? = null,
    val pendingMessageDraft: String? = null,
    val pendingUploadIds: List<String> = emptyList(),
    val pendingDownloadIds: List<String> = emptyList(),
    val meshStatus: String? = null
)

@Singleton
class StateRestorationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "StateRestoration"
        private val STATE_KEY = stringPreferencesKey("app_restoration_state")
    }

    val stateFlow: Flow<RestorationState> = context.recoveryDataStore.data.map { prefs ->
        val jsonString = prefs[STATE_KEY]
        if (jsonString != null) {
            try {
                Json.decodeFromString<RestorationState>(jsonString)
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to decode restoration state", e)
                RestorationState()
            }
        } else {
            RestorationState()
        }
    }

    suspend fun saveState(state: RestorationState) {
        try {
            val jsonString = Json.encodeToString(state)
            context.recoveryDataStore.edit { prefs ->
                prefs[STATE_KEY] = jsonString
            }
            MeshLogger.d(TAG, "State saved successfully: ${state.lastScreen}")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to save restoration state", e)
        }
    }

    suspend fun updateState(update: (RestorationState) -> RestorationState) {
        try {
            context.recoveryDataStore.edit { prefs ->
                val currentJson = prefs[STATE_KEY]
                val currentState = currentJson?.let {
                    try {
                        Json.decodeFromString<RestorationState>(it)
                    } catch (e: Exception) {
                        RestorationState()
                    }
                } ?: RestorationState()
                
                val newState = update(currentState)
                prefs[STATE_KEY] = Json.encodeToString(newState)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to update restoration state", e)
        }
    }
    
    suspend fun clearState() {
        try {
            context.recoveryDataStore.edit { prefs ->
                prefs.remove(STATE_KEY)
            }
            MeshLogger.d(TAG, "State cleared successfully")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to clear restoration state", e)
        }
    }
}
