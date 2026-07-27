package com.meshlink.voice.api

interface VoiceTransport {
    suspend fun startVoiceCall(peerId: String)
    suspend fun endVoiceCall(peerId: String)
}
