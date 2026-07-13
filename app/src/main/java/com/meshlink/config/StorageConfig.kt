package com.meshlink.config

object StorageConfig {
    const val DATABASE_VERSION = 2
    const val DATABASE_NAME = "meshlink_db"
    const val PREFS_NAME = "meshlink_prefs"
    const val ENCRYPTED_PREFS_NAME = "meshlink_encrypted_prefs"
    
    // Cache quotas
    const val MAX_THUMB_CACHE_SIZE_BYTES = 50 * 1024 * 1024L // 50MB
    const val MEDIA_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
}
