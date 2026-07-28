package com.meshlink.logging

/**
 * Standardized constant strings for common event names
 * to avoid free-form strings where applicable.
 */
object LogEventNames {
    const val PACKET_RECEIVED = "PACKET_RECEIVED"
    const val PACKET_SENT = "PACKET_SENT"
    const val CONNECTION_ESTABLISHED = "CONNECTION_ESTABLISHED"
    const val CONNECTION_LOST = "CONNECTION_LOST"
    const val ROUTE_DISCOVERED = "ROUTE_DISCOVERED"
    const val ROUTE_FAILED = "ROUTE_FAILED"
    const val SESSION_STARTED = "SESSION_STARTED"
    const val SESSION_ENDED = "SESSION_ENDED"
    const val MESSAGE_DELIVERED = "MESSAGE_DELIVERED"
    const val SECURITY_HANDSHAKE_STARTED = "SECURITY_HANDSHAKE_STARTED"
    const val SECURITY_HANDSHAKE_COMPLETED = "SECURITY_HANDSHAKE_COMPLETED"
    const val SECURITY_HANDSHAKE_FAILED = "SECURITY_HANDSHAKE_FAILED"
    const val ERROR_OCCURRED = "ERROR_OCCURRED"
}
