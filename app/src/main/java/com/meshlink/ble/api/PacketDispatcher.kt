package com.meshlink.ble.api

import com.meshlink.domain.model.MeshPacket

interface PacketDispatcher {
    /**
     * Dispatches a single packet to the target peer.
     * @param targetPeerId The canonical ID of the target peer.
     * @param packet The packet to dispatch.
     * @return true if successfully routed/dispatched, false otherwise.
     */
    suspend fun dispatchSinglePacket(targetPeerId: String, packet: MeshPacket): Boolean
}
