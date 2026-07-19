package com.meshlink.common.logger

object PrivacyLogInterceptor {

    private val MAC_ADDRESS_REGEX = Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
    private val IPV4_REGEX = Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")
    
    fun redact(message: String): String {
        var redacted = message
        
        // Redact MAC addresses
        redacted = MAC_ADDRESS_REGEX.replace(redacted, "[REDACTED_MAC]")
        
        // Redact IPv4 addresses
        redacted = IPV4_REGEX.replace(redacted, "[REDACTED_IP]")
        
        return redacted
    }

    fun redactMetadata(metadata: Map<String, String>?): Map<String, String> {
        if (metadata == null) return emptyMap()
        val result = mutableMapOf<String, String>()
        for ((key, value) in metadata) {
            val lowerKey = key.lowercase()
            if (lowerKey.contains("token") || lowerKey.contains("key") || lowerKey.contains("secret") || lowerKey.contains("password")) {
                result[key] = "[REDACTED_SENSITIVE]"
            } else {
                result[key] = redact(value)
            }
        }
        return result
    }
}
