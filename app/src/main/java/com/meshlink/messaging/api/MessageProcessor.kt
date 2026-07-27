package com.meshlink.messaging.api

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult

/**
 * Processes incoming and outgoing chat messages.
 *
 * Responsibility: Handle message parsing, saving to repository, and delegating to router.
 * Lifecycle: Application scoped.
 * Thread Safety: Implementations must be thread-safe.
 * Return Contract: Processing operations return [MeshResult] to indicate success or failure.
 * Failure Conditions: Invalid packet format, routing failure.
 */
interface MessageProcessor {
    @Deprecated("Use processPacket instead", ReplaceWith("processPacket(packet)"))
    suspend fun processIncomingPacket(packet: MeshPacket)

    /**
     * Processes an incoming mesh packet.
     *
     * @param packet The received packet.
     * @return [MeshResult.Success] if processed successfully, [MeshResult.Error] on failure.
     */
    suspend fun processPacket(packet: MeshPacket): MeshResult<Unit>

    @Deprecated("Use sendMessage instead", ReplaceWith("sendMessage(destinationId, payload)"))
    suspend fun sendOutgoingMessage(destinationId: String, payload: String)

    /**
     * Sends an outgoing message to a destination.
     *
     * @param destinationId The destination mesh ID.
     * @param payload The message content.
     * @return [MeshResult.Success] if sent successfully, [MeshResult.Error] on failure.
     */
    suspend fun sendMessage(destinationId: String, payload: String): MeshResult<Unit>
}
