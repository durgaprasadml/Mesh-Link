package com.meshlink.ble.data


import com.meshlink.ble.discovery.DiscoveryEngine
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
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
    private val meshRouter: Router,
    private val connectionManager: BleConnectionManager,
    private val discoveryManager: DiscoveryManager
) {
    private val TAG = "RoutingCoordinator"


    fun resolvePeerAddress(peerIdOrAddress: String): String? {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) return peerIdOrAddress

        val norm = com.meshlink.util.MeshIdNormalizer.canonicalize(peerIdOrAddress)
        
        val scanned = discoveryManager.scannedDevices.value.values.firstOrNull { com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) == norm }
        if (scanned != null) return scanned.address

        val record = discoveryManager.discoveryEngine.cache.getAll().firstOrNull {
            com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) == norm
        }
        if (record != null && record.macAddress.isNotBlank()) return record.macAddress
        
        val route = meshRouter.routeTable[peerIdOrAddress] ?: meshRouter.routeTable[norm]
        if (route != null) return route.nextHop

        return null
    }

    fun resolveMeshId(addressOrMeshId: String): String? {
        if (!BleConstants.isBluetoothAddress(addressOrMeshId)) {
            return com.meshlink.util.MeshIdNormalizer.canonicalize(addressOrMeshId)
        }

        val scanned = discoveryManager.scannedDevices.value[addressOrMeshId]
            ?: discoveryManager.scannedDevices.value.values.firstOrNull { it.address.equals(addressOrMeshId, ignoreCase = true) }
        if (scanned != null && scanned.meshId.isNotBlank()) {
            return com.meshlink.util.MeshIdNormalizer.canonicalize(scanned.meshId)
        }

        val record = discoveryManager.discoveryEngine.cache.get(addressOrMeshId)
        if (record != null && record.meshId.isNotBlank()) {
            return com.meshlink.util.MeshIdNormalizer.canonicalize(record.meshId)
        }

        return null
    }

    fun hasDeliveryPath(targetPeerIdOrAddress: String): Boolean {
        if (targetPeerIdOrAddress == "BROADCAST") return true
        val routeAddress = meshRouter.routeTable[targetPeerIdOrAddress]?.nextHop
        if (routeAddress != null) return true
        return resolvePeerAddress(targetPeerIdOrAddress) != null
    }

    fun isDirectlyConnected(peerIdOrAddress: String): Boolean {
        if (BleConstants.isBluetoothAddress(peerIdOrAddress)) return true
        val norm = com.meshlink.util.MeshIdNormalizer.canonicalize(peerIdOrAddress)
        val scanned = discoveryManager.scannedDevices.value.values.firstOrNull { com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) == norm }
        if (scanned != null) return true
        val route = meshRouter.routeTable[peerIdOrAddress] ?: meshRouter.routeTable[norm]
        if (route != null) return route.hops <= 0
        return false
    }
}
