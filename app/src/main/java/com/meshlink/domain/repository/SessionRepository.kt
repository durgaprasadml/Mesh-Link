package com.meshlink.domain.repository

import com.meshlink.domain.model.PeerSecureSession

interface SessionRepository {
    suspend fun getSession(peerId: String): PeerSecureSession?
    suspend fun saveSession(session: PeerSecureSession)
}
