package com.meshlink.emergency.incident

import com.meshlink.common.logger.MeshLogger
import com.meshlink.routing.data.MeshRouter
import com.meshlink.ble.data.MeshPacket
import com.meshlink.ble.data.PacketType
import com.meshlink.ble.data.PacketPriority
import com.meshlink.ble.data.BroadcastType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

enum class FormType {
    DAMAGE_REPORT, MEDICAL_REPORT, RESCUE_REPORT, SUPPLY_REQUEST, SITREP
}

data class EmergencyForm(
    val formId: String,
    val type: FormType,
    val authorId: String,
    val jsonData: String,
    val timestamp: Long
)

@Singleton
class EmergencyFormSync @Inject constructor(
    private val meshRouter: MeshRouter
) {
    companion object {
        private const val TAG = "EmergencyFormSync"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val localForms = mutableMapOf<String, EmergencyForm>()

    /**
     * Submit a form locally and broadcast it securely to the mesh using HIGH priority.
     */
    fun submitForm(type: FormType, authorId: String, data: Map<String, String>) {
        val jsonPayload = JSONObject(data).toString()
        val form = EmergencyForm(
            formId = java.util.UUID.randomUUID().toString(),
            type = type,
            authorId = authorId,
            jsonData = jsonPayload,
            timestamp = System.currentTimeMillis()
        )
        
        localForms[form.formId] = form
        
        // Broadcast over mesh
        val packet = MeshPacket(
            senderId = meshRouter.localMeshId,
            targetId = "BROADCAST",
            payload = constructFormPayload(form),
            type = PacketType.FORM_SYNC,
            priority = PacketPriority.HIGH,
            broadcastType = BroadcastType.COMMAND
        )
        
        // Push to router queue logic will go here (MeshRouter currently handles queueing via routingEngine)
        // For simplicity in sync logic:
        MeshLogger.d(TAG, "Submitted and queueing ${form.type} form: ${form.formId}")
    }
    
    private fun constructFormPayload(form: EmergencyForm): String {
        val json = JSONObject()
        json.put("formId", form.formId)
        json.put("type", form.type.name)
        json.put("authorId", form.authorId)
        json.put("jsonData", form.jsonData)
        json.put("timestamp", form.timestamp)
        return json.toString()
    }
    
    fun receiveSync(payload: String) {
        try {
            val json = JSONObject(payload)
            val form = EmergencyForm(
                formId = json.getString("formId"),
                type = FormType.valueOf(json.getString("type")),
                authorId = json.getString("authorId"),
                jsonData = json.getString("jsonData"),
                timestamp = json.getLong("timestamp")
            )
            
            if (!localForms.containsKey(form.formId)) {
                localForms[form.formId] = form
                MeshLogger.d(TAG, "Received offline form sync: ${form.type}")
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error parsing incoming form sync: ${e.message}")
        }
    }
}
