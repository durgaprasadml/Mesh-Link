package com.meshlink.routing.data.source

import com.meshlink.ble.data.MeshPacket
import kotlinx.coroutines.flow.SharedFlow

interface RoutingDataSource {
    var localMeshId: String
    
    fun processIncomingPayload(payloadStr: String, senderAddress: String, isWifi: Boolean)
    fun processOutgoingPacket(packet: MeshPacket, dispatchBlock: (String, String) -> Boolean): Boolean
    
    val incomingPayloads: SharedFlow<Pair<String, MeshPacket>>
}
