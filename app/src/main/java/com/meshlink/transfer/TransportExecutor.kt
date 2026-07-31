package com.meshlink.transfer

import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.model.PacketType
import com.meshlink.transport.HybridTransport
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportExecutor @Inject constructor(
    private val hybridTransport: HybridTransport
) {
    companion object {
        private const val TAG = "TransportExecutor"
    }

    var onPacketDispatched: ((MeshPacket, TransportType) -> Unit)? = null

    /**
     * Determines the optimal route for a transfer session packet.
     */
    fun selectRouteForTransfer(
        targetId: String,
        packetType: PacketType,
        payloadSize: Long
    ): TransportType {
        val routeType = hybridTransport.getSelectedRouteType(
            targetId = targetId,
            packetType = packetType,
            payloadSize = payloadSize
        )
        return when (routeType) {
            com.meshlink.domain.model.RouteType.WIFI_DIRECT -> TransportType.WIFI_DIRECT
            com.meshlink.domain.model.RouteType.HYBRID -> TransportType.HYBRID
            else -> TransportType.BLE
        }
    }

    /**
     * Sends a packet through HybridTransport and handles fallback automatically.
     */
    suspend fun dispatchPacket(packet: MeshPacket): Pair<MeshResult<Unit>, TransportType> {
        val payloadSize = packet.payload.length.toLong()
        val preferredTransport = selectRouteForTransfer(packet.targetId, packet.type, payloadSize)

        MeshLogger.d(TAG, "Dispatching packet ${packet.packetId} (type=${packet.type}, target=${packet.targetId.takeLast(6)}) via preferred $preferredTransport")

        val result = hybridTransport.sendPacket(packet)

        val actualTransport = if (result is MeshResult.Success) {
            preferredTransport
        } else {
            MeshLogger.w(TAG, "Primary transport send failed for ${packet.packetId}. Executing fallback BLE send...")
            TransportType.BLE
        }

        onPacketDispatched?.invoke(packet, actualTransport)
        return Pair(result, actualTransport)
    }
}
