package com.meshlink.enterprise.governance

import javax.inject.Inject
import javax.inject.Singleton

enum class DataClassification {
    PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED, HIGHLY_RESTRICTED
}

@Singleton
class PrivacyManager @Inject constructor() {

    fun sanitizeExportPayload(payload: Map<String, Any>, maxAllowedClassification: DataClassification): Map<String, Any> {
        val sanitized = mutableMapOf<String, Any>()
        
        for ((key, value) in payload) {
            val classification = classifyKey(key)
            if (classification <= maxAllowedClassification) {
                sanitized[key] = value
            } else {
                sanitized[key] = "[REDACTED - PRIVACY GOVERNANCE]"
            }
        }
        return sanitized
    }

    private fun classifyKey(key: String): DataClassification {
        return when {
            key.contains("mac_address", ignoreCase = true) -> DataClassification.HIGHLY_RESTRICTED
            key.contains("location", ignoreCase = true) -> DataClassification.RESTRICTED
            key.contains("node_id", ignoreCase = true) -> DataClassification.CONFIDENTIAL
            key.contains("battery", ignoreCase = true) -> DataClassification.INTERNAL
            else -> DataClassification.PUBLIC
        }
    }
}
