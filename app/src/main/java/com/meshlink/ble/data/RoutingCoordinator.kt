package com.meshlink.ble.data


import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.data.MeshRouter
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.TrustManager
import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingCoordinator @Inject constructor(
    private val userRepository: UserRepository,
    private val cryptoManager: MeshCryptoManager,
    private val trustManager: TrustManager,
    private val sessionManager: SessionManager,
    private val rekeyManager: RekeyManager,
    private val meshRouter: MeshRouter,
    private val connectionManager: BleConnectionManager,
    private val discoveryManager: DiscoveryManager
) {
    private val TAG = "RoutingCoordinator"

    fun networkId(peerId: String): String = BleConstants.toNetworkId(peerId)

    fun normalizePeerId(peerIdOrAddress: String): String {
        val withoutColons = peerIdOrAddress.replace(":", "").uppercase()
        return if (withoutColons.length > 8) withoutColons.takeLast(8) else withoutColons
    }

    fun incomingChatId(senderMeshId: String): String = normalizePeerId(senderMeshId)
    fun outgoingChatId(targetMeshId: String): String = normalizePeerId(targetMeshId)
    fun resolveChatId(peerIdOrAddress: String): String = normalizePeerId(peerIdOrAddress)

    fun resolvePeerAddress(peerIdOrAddress: String): String? {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) return peerIdOrAddress

        val norm = normalizePeerId(peerIdOrAddress)
        
        val scanned = discoveryManager.scannedDevices.value.values.firstOrNull { normalizePeerId(it.meshId) == norm }
        if (scanned != null) return scanned.address
        
        val route = meshRouter.routeTable[peerIdOrAddress] ?: meshRouter.routeTable[norm]
        if (route != null) return route.nextHop

        return null
    }

    fun hasDeliveryPath(targetPeerIdOrAddress: String): Boolean {
        if (targetPeerIdOrAddress == "BROADCAST") return true
        val routeAddress = meshRouter.routeTable[targetPeerIdOrAddress]?.nextHop
        if (routeAddress != null) return true
        return resolvePeerAddress(targetPeerIdOrAddress) != null
    }
}
