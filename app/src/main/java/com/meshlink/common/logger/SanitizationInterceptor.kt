package com.meshlink.common.logger

import java.util.regex.Pattern

object SanitizationInterceptor {
    private val MAC_ADDRESS_PATTERN = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
    private val IP_ADDRESS_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")
    // Very basic checks for keys/secrets in logs
    private val SENSITIVE_KEYWORDS = listOf("password", "passphrase", "secret", "privateKey", "aesKey", "token")

    fun sanitize(message: String): String {
        var sanitized = message
        
        // Redact MAC addresses
        sanitized = MAC_ADDRESS_PATTERN.matcher(sanitized).replaceAll("XX:XX:XX:XX:XX:XX")
        
        // Redact IP addresses
        sanitized = IP_ADDRESS_PATTERN.matcher(sanitized).replaceAll("XXX.XXX.XXX.XXX")
        
        // Redact sensitive keyword values (basic heuristic)
        SENSITIVE_KEYWORDS.forEach { keyword ->
            val regex = "(?i)($keyword)\\s*[:=]\\s*([^\\s]+)".toRegex()
            sanitized = sanitized.replace(regex, "$1=***REDACTED***")
        }

        return sanitized
    }

    fun sanitizeMetadata(metadata: Map<String, String>?): Map<String, String> {
        if (metadata == null) return emptyMap()
        
        val sanitizedMetadata = mutableMapOf<String, String>()
        metadata.forEach { (key, value) ->
            if (SENSITIVE_KEYWORDS.any { key.contains(it, ignoreCase = true) }) {
                sanitizedMetadata[key] = "***REDACTED***"
            } else {
                sanitizedMetadata[key] = sanitize(value)
            }
        }
        return sanitizedMetadata
    }
}
