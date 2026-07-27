package com.meshlink.logging

/**
 * Contextual correlation IDs associated with a log event.
 */
data class LogContext(
    val nodeId: String? = null,
    val peerId: String? = null,
    val packetId: String? = null,
    val traceId: String? = null,
    val sessionId: String? = null,
    val route: String? = null,
    val transport: String? = null
) {
    fun withNode(id: String) = copy(nodeId = id)
    fun withPeer(id: String) = copy(peerId = id)
    fun withPacket(id: String) = copy(packetId = id)
    fun withTrace(id: String) = copy(traceId = id)
    fun withSession(id: String) = copy(sessionId = id)
    fun withRoute(routePath: String) = copy(route = routePath)
    fun withTransport(transportType: String) = copy(transport = transportType)

    companion object {
        val EMPTY = LogContext()
    }
}
