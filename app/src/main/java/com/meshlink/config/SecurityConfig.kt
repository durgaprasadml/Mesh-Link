package com.meshlink.config

object SecurityConfig {
    const val REKEY_INTERVAL_MS = 86400000L // 24 hours
    const val SESSION_TIMEOUT_MS = 3600000L // 1 hour
    const val PBKDF2_ITERATIONS = 100000
    const val TRUST_THRESHOLD_HIGH = 90
    const val TRUST_THRESHOLD_LOW = 30
}
