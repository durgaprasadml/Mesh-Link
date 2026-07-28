package com.meshlink.ble.api

import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.DispatchResult

interface PacketDispatcher {
    /**
     * Dispatches a single packet to the target peer.
     * @param targetPeerId The canonical ID of the target peer.
     * @param packet The packet to dispatch.
     * @return DispatchResult indicating queue admission or error.
     */
    suspend fun dispatchSinglePacket(targetPeerId: String, packet: MeshPacket): DispatchResult
}
