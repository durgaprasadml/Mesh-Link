package com.meshlink.ble.data

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.repository.UserRepository
import com.meshlink.routing.api.Router
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.policy.EncryptionRequirement
import com.meshlink.security.policy.PacketEncryptionPolicy
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorePacketDispatcher @Inject constructor(
    private val userRepository: UserRepository,
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager,
    private val meshRouter: Router,
    private val routingCoordinator: RoutingCoordinator,
    private val connectionManager: BleConnectionManager,
    private val discoveryManager: DiscoveryManager
) : PacketDispatcher {
    private val TAG = "CorePacketDispatcher"

    override suspend fun dispatchSinglePacket(targetPeerId: String, packet: MeshPacket): Boolean {
        connectToPeer(targetPeerId)
        connectToAllScannedDevices()
        
        var packetToSend = packet
        if (!packetToSend.encrypted && targetPeerId != "BROADCAST") {
            val requirement = PacketEncryptionPolicy.getRequirement(packetToSend.type)
            val shouldEncrypt = when (requirement) {
                EncryptionRequirement.REQUIRED -> true
                EncryptionRequirement.OPTIONAL -> userRepository.isEncryptionEnabled.first()
                EncryptionRequirement.BOOTSTRAP_ONLY -> false
            }

            if (shouldEncrypt) {
                val result = encryptAndWrapPayload(packetToSend.payload, targetPeerId, true, packetToSend.packetId)
                if (result != null) {
                    val (encPayload, isEnc) = result
                    packetToSend = packetToSend.copy(payload = encPayload, encrypted = isEnc)
                } else {
                    MeshLogger.e(TAG, "Centralized encryption failed for packet type: ${packetToSend.type}")
                    return false
                }
            }
        }

        meshRouter.routeMediaPacket(packetToSend)
        return true
    }

    private fun connectToDevice(address: String) {
        if (com.meshlink.ble.data.BleConstants.isBluetoothAddress(address)) {
            connectionManager.connectToDevice(address)
        } else {
            val resolved = routingCoordinator.resolvePeerAddress(address)
            if (resolved != null) {
                connectionManager.connectToDevice(resolved)
            } else {
                MeshLogger.w(TAG, "Cannot directly connect to $address - MAC unknown. Relying on mesh routing.")
            }
        }
    }

    private fun connectToPeer(peerIdOrAddress: String): Boolean {
        val address = routingCoordinator.resolvePeerAddress(peerIdOrAddress) ?: return false
        return try {
            connectToDevice(address)
            true
        } catch (e: Exception) {
            MeshLogger.w(TAG, "connectToPeer failed for $peerIdOrAddress: ${e.message}")
            false
        }
    }

    private fun connectToAllScannedDevices() {
        if (!discoveryManager.isScanning() && !discoveryManager.isAdvertising()) {
            return
        }

        val devices = discoveryManager.scannedDevices.value.values
        if (devices.isEmpty()) return

        devices.forEach { device ->
            try {
                connectionManager.connectToDevice(device.address, isManual = false)
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Auto-connect request failed for ${device.name}: ${e.message}")
            }
        }
    }

    fun encryptAndWrapPayload(
        plaintext: String,
        targetPeerId: String,
        requireEncryption: Boolean,
        messageId: String
    ): Pair<String, Boolean>? {
        var finalPlaintext = plaintext
        var aadBytes: ByteArray? = null
        var aadPrefix = ""

        if (requireEncryption) {
            val aadResult = sessionManager.generateAad(targetPeerId)
            if (aadResult != null) {
                aadBytes = aadResult.first
                aadPrefix = aadResult.second
            }
        }

        val result = cryptoManager.encryptOrPassthrough(finalPlaintext, targetPeerId, requireEncryption, messageId, 0, aadBytes)
            ?: return null

        val (ciphertext, isEncrypted) = result
        if (isEncrypted && aadPrefix.isNotEmpty()) {
            return Pair("$aadPrefix$ciphertext", true)
        }
        return result
    }
}
