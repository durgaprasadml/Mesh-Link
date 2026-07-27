package com.meshlink.media.api

interface MediaTransfer {
    suspend fun sendMedia(uri: String, peerId: String)
}
