package com.meshlink.emergency.medical

import com.meshlink.common.logger.MeshLogger
import com.meshlink.security.data.TrustManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MedicalTag(
    val userId: String,
    val bloodGroup: String,
    val allergies: String,
    val existingConditions: String,
    val emergencyContact: String,
    val isEncrypted: Boolean = true
)

@Singleton
class MedicalManager @Inject constructor(
    private val trustManager: TrustManager
) {
    companion object {
        private const val TAG = "MedicalManager"
    }

    // Maps userId to their MedicalTag
    private val _medicalTags = MutableStateFlow<Map<String, MedicalTag>>(emptyMap())
    val medicalTags: StateFlow<Map<String, MedicalTag>> = _medicalTags.asStateFlow()
    
    fun updateLocalMedicalTag(tag: MedicalTag) {
        val current = _medicalTags.value.toMutableMap()
        current[tag.userId] = tag
        _medicalTags.value = current
        MeshLogger.d(TAG, "Updated local medical tag for ${tag.userId}")
    }

    /**
     * Decrypts and stores a medical tag received over the mesh, but ONLY if the sender
     * has sufficient trust level (COMMAND or MEDIC).
     */
    fun receiveMedicalTag(senderId: String, tag: MedicalTag) {
        // Simple mock trust check
        val trust = trustManager.getTrustLevel(senderId)
        if (trust == com.meshlink.security.data.TrustLevel.VERIFIED) {
            val current = _medicalTags.value.toMutableMap()
            current[tag.userId] = tag
            _medicalTags.value = current
            MeshLogger.d(TAG, "Stored remote medical tag for ${tag.userId} from trusted sender $senderId")
        } else {
            MeshLogger.w(TAG, "Rejected medical tag from untrusted sender $senderId")
        }
    }
}
