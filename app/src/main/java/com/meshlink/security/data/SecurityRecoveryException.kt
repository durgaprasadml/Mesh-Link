package com.meshlink.security.data



/**
 * Thrown when cryptographic material is permanently lost or invalidated
 * (e.g. Android Keystore key permanently invalidated), requiring a controlled
 * error response to prevent destructive fallback migration loops.
 */
class SecurityRecoveryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
