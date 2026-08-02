package com.meshlink.security.data.source



interface SessionDataSource {
    fun hasActiveSession(peerId: String): Boolean
    fun establishSession(peerId: String)
    fun terminateSession(peerId: String)
}
