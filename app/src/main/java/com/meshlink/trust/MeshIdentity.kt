package com.meshlink.trust

/**
 * Immutable cryptographic identity model for Mesh-Link network node.
 * Identity is defined strictly by Mesh ID + Public Key.
 * Display Name is editable metadata and not identity anchor.
 */
data class MeshIdentity(
    val meshId: String,
    val publicKey: String,
    val identityVersion: Int = 1,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val displayName: String,
    val signature: String = ""
) {
    /**
     * Payload string used for cryptographic signing and signature verification.
     */
    fun toSigningPayload(): String {
        return "$meshId:$publicKey:$identityVersion:$creationTimestamp"
    }

    /**
     * Serializes identity to JSON string.
     */
    fun toJson(): String {
        return """{"meshId":"$meshId","publicKey":"$publicKey","identityVersion":$identityVersion,"creationTimestamp":$creationTimestamp,"displayName":"$displayName","signature":"$signature"}"""
    }

    companion object {
        fun fromJson(jsonStr: String): MeshIdentity {
            val meshId = extractJsonValue(jsonStr, "meshId")
            val publicKey = extractJsonValue(jsonStr, "publicKey")
            val version = extractJsonValue(jsonStr, "identityVersion").toIntOrNull() ?: 1
            val timestamp = extractJsonValue(jsonStr, "creationTimestamp").toLongOrNull() ?: System.currentTimeMillis()
            val displayName = extractJsonValue(jsonStr, "displayName")
            val signature = extractJsonValue(jsonStr, "signature")

            return MeshIdentity(
                meshId = meshId,
                publicKey = publicKey,
                identityVersion = version,
                creationTimestamp = timestamp,
                displayName = if (displayName.isEmpty()) "User" else displayName,
                signature = signature
            )
        }

        private fun extractJsonValue(json: String, key: String): String {
            val pattern = """"$key"\s*:\s*("(.*?)"|(\d+))""".toRegex()
            val match = pattern.find(json) ?: return ""
            return match.groupValues[2].ifEmpty { match.groupValues[3] }
        }
    }
}
