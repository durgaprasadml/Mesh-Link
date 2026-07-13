package com.meshlink.config

object MediaConfig {
    const val CHUNK_SIZE_BYTES = 1024 * 512 // 512KB
    const val MAX_IMAGE_SIZE_MB = 10
    const val MAX_VOICE_DURATION_SEC = 60
    const val VOICE_BITRATE = 16000
    const val MAX_RETRIES = 3
}
