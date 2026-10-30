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

        // 1. Prefer an address that is currently connected in connectionManager
        val activeClientAddress = connectionManager.activeClients.firstOrNull { clientAddr ->
            resolveMeshId(clientAddr) == norm
        }
        if (activeClientAddress != null) return activeClientAddress

        // 2. Next, look for the freshest scanned device (highest lastSeen)
        val freshestScanned = discoveryManager.scannedDevices.value.values
            .filter { com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) == norm }
            .maxByOrNull { it.lastSeen }
        if (freshestScanned != null) return freshestScanned.address

        // 3. Next, look in discovery cache for the active link or freshest record
        val records = discoveryManager.discoveryEngine.cache.getAll()
            .filter { com.meshlink.util.MeshIdNormalizer.canonicalize(it.meshId) == norm && it.macAddress.isNotBlank() }
        val activeOrFreshestRecord = records.firstOrNull { it.linkActive } ?: records.maxByOrNull { it.lastSeenMillis }
        if (activeOrFreshestRecord != null) return activeOrFreshestRecord.macAddress
        
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
