package com.meshlink.security.data

import android.util.Base64
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.di.DefaultDispatcher
import com.meshlink.domain.repository.UserRepository
import java.security.PrivateKey
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Singleton
class RekeyManager @Inject constructor(
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val maintenanceScheduler: com.meshlink.common.maintenance.MaintenanceScheduler
,
    @com.meshlink.di.ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope) {
    private val TAG = "RekeyManager"
// Map of peerId -> PendingRekey state
    private val pendingRekeys = ConcurrentHashMap<String, PendingRekey>()

    data class PendingRekey(
        val myEphemeralPrivateKey: PrivateKey,
        val myEphemeralPublicKeyBase64: String,
        val nextKeyVersion: Int,
        val timestamp: Long,
        var retries: Int = 0
    )

    // Callbacks to MeshRepository (to avoid circular dependency loop)
    var sendPacketCallback: (suspend (peerId: String, packet: MeshPacket) -> Unit)? = null
    var forceKeyExchangeCallback: ((peerId: String) -> Unit)? = null

    init {
        maintenanceScheduler.schedule("RekeySweep", 60_000L) {
            val now = System.currentTimeMillis()
            
            val user = userRepository.getLocalUser()
            val myId = user?.meshId ?: return@schedule

            for (peerId in sessionManager.getAllSessionPeers()) {
                val session = sessionManager.getSession(peerId) ?: continue
                
                // Cleanup Grace Window (Transition window limit is 60s)
                if (session.previousKeyVersion != 0 && now > session.rekeyTimestamp + 60_000) {
                    MeshLogger.d(TAG, "Grace period ended for peer $peerId, deleting previous AES key")
                    cryptoManager.clearPreviousSharedKey(peerId)
                    session.previousKeyVersion = 0
                }

                // Check if we are the deterministic initiator
                if (myId <= peerId) {
                    continue // We are not the initiator, we just respond.
                }

                // Conditions for automatic rekey
                val sessionAge = now - session.sessionStart
                val packetCount = session.totalEncryptedPackets.get()
                
                if (sessionAge > 30 * 60 * 1000L || packetCount > 10_000) {
                    val reason = if (packetCount > 10_000) "packet_limit" else "session_age"
                    initiateRekey(peerId, reason)
                }

                // Retry pending rekeys if timeout (5 seconds)
                val pending = pendingRekeys[peerId]
                if (pending != null && now > pending.timestamp + 5000) {
                    if (pending.retries < 3) {
                        pending.retries++
                        MeshLogger.w(TAG, "Rekey timed out for $peerId. Retrying (${pending.retries}/3)...")
                        sendRekeyPacket(peerId, session.sessionId, session.keyVersion, pending.nextKeyVersion, pending.myEphemeralPublicKeyBase64)
                    } else {
                        MeshLogger.e(TAG, "Rekey failed 3 times for $peerId. Falling back to full handshake.")
                        destroyPendingRekey(peerId)
                        forceKeyExchangeCallback?.invoke(peerId)
                    }
                }
            }
        }
    }



    fun manualRekey(peerId: String) {
        initiateRekey(peerId, "manual")
    }

    private fun initiateRekey(peerId: String, reason: String) {
        if (pendingRekeys.containsKey(peerId)) return // Already in progress
        
        val session = sessionManager.getSession(peerId) ?: return
        MeshLogger.d(TAG, "Initiating secure rekey for $peerId (Reason: $reason)")
        
        val ephemeralKeyPair = cryptoManager.generateEphemeralKeyPair()
        val nextKv = session.keyVersion + 1
        
        val pending = PendingRekey(
            myEphemeralPrivateKey = ephemeralKeyPair.private,
            myEphemeralPublicKeyBase64 = android.util.Base64.encodeToString(ephemeralKeyPair.public.encoded, android.util.Base64.NO_WRAP),
            nextKeyVersion = nextKv,
            timestamp = System.currentTimeMillis()
        )
        pendingRekeys[peerId] = pending

        applicationScope.launch(defaultDispatcher) {
            sendRekeyPacket(peerId, session.sessionId, session.keyVersion, nextKv, pending.myEphemeralPublicKeyBase64)
        }
    }

    private suspend fun sendRekeyPacket(peerId: String, sessionId: String, currentKv: Int, nextKv: Int, ephemeralPubBase64: String) {
        try {
            val timestamp = System.currentTimeMillis()
            val nonce = UUID.randomUUID().toString().substring(0, 8)
            val dataToSign = "$sessionId|$currentKv|$nextKv|$ephemeralPubBase64|$timestamp|$nonce".toByteArray(Charsets.UTF_8)
            
            val user = userRepository.getLocalUser()
            if (user != null) {
                val signature = cryptoManager.sign(dataToSign)
                val signatureBase64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)
                val payload = "rekey|$sessionId|$currentKv|$nextKv|$ephemeralPubBase64|$timestamp|$nonce|$signatureBase64"
                
                val packet = MeshPacket(
                    senderId = "", // Filled by MeshRepository
                    targetId = peerId,
                    payload = payload,
                    type = PacketType.SESSION_REKEY,
                    encrypted = false
                )
                sendPacketCallback?.invoke(peerId, packet)
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to send rekey packet: ${e.message}")
        }
    }

    fun handleRekeyPacket(peerId: String, payload: String, senderPublicKey: String?) {
        try {
            val parts = payload.split("|")
            if (parts.size != 8 || parts[0] != "rekey") return

            val sessionId = parts[1]
            val currentKv = parts[2].toInt()
            val nextKv = parts[3].toInt()
            val ephemeralPubBase64 = parts[4]
            val timestamp = parts[5].toLong()
            val nonce = parts[6]
            val signatureBase64 = parts[7]

            val session = sessionManager.getSession(peerId)
            if (session == null || session.sessionId != sessionId) {
                MeshLogger.e(TAG, "Rekey failed: Session mismatch")
                return
            }

            val dataToVerify = "$sessionId|$currentKv|$nextKv|$ephemeralPubBase64|$timestamp|$nonce".toByteArray(Charsets.UTF_8)
            val sigBytes = android.util.Base64.decode(signatureBase64, android.util.Base64.NO_WRAP)
            val signingKey = cryptoManager.getPeerSigningKey(peerId) ?: senderPublicKey
            
            if (signingKey == null || !cryptoManager.verifySignature(signingKey, dataToVerify, sigBytes)) {
                MeshLogger.e(TAG, "Rekey failed: Invalid signature")
                return
            }

            val pending = pendingRekeys[peerId]

            if (pending != null && pending.nextKeyVersion == nextKv) {
                // We are the INITIATOR receiving the RESPONDER's reply
                cryptoManager.deriveEphemeralSharedKey(peerId, ephemeralPubBase64, pending.myEphemeralPrivateKey)
                
                // Securely destroy the private key memory
                destroyPendingRekey(peerId)

                // Update session
                session.previousKeyVersion = session.keyVersion
                session.keyVersion = nextKv
                session.rekeyTimestamp = System.currentTimeMillis()
                session.rotationReason = "initiator_success"

                MeshLogger.d(TAG, "✅ Secure rekey successful (Initiator). New Key Version: $nextKv")
            } else {
                // We are the RESPONDER
                val ephemeralKeyPair = cryptoManager.generateEphemeralKeyPair()

                applicationScope.launch(defaultDispatcher) {
                    // Send back our ephemeral public key
                    val myEphemeralPubBase64 = android.util.Base64.encodeToString(ephemeralKeyPair.public.encoded, android.util.Base64.NO_WRAP)
                    sendRekeyPacket(peerId, sessionId, currentKv, nextKv, myEphemeralPubBase64)

                    // Fix CRITICAL-05 Race Condition: Wait for the packet to be fully dispatched and encrypted
                    // BEFORE we rotate the keys. This ensures the reply is encrypted with currentKv.
                    cryptoManager.deriveEphemeralSharedKey(peerId, ephemeralPubBase64, ephemeralKeyPair.private)

                    // Attempt to securely destroy our ephemeral private key.
                    // Note: Standard Android JCA providers often throw DestroyFailedException here.
                    try {
                        if (!ephemeralKeyPair.private.isDestroyed) {
                            ephemeralKeyPair.private.destroy()
                        }
                    } catch (e: javax.security.auth.DestroyFailedException) {
                        MeshLogger.d(TAG, "JVM Limitation: Secure erasure of ephemeral private key not supported by provider.")
                    } catch (e: UnsupportedOperationException) {
                        MeshLogger.d(TAG, "JVM Limitation: Secure erasure of ephemeral private key not supported by provider.")
                    }

                    session.previousKeyVersion = session.keyVersion
                    session.keyVersion = nextKv
                    session.rekeyTimestamp = System.currentTimeMillis()
                    session.rotationReason = "responder_success"

                    MeshLogger.d(TAG, "✅ Secure rekey successful (Responder). New Key Version: $nextKv")
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to handle rekey packet: ${e.message}")
        }
    }

    private fun destroyPendingRekey(peerId: String) {
        val pending = pendingRekeys.remove(peerId)
        if (pending != null) {
            // Attempt to securely destroy the ephemeral private key.
            try {
                if (!pending.myEphemeralPrivateKey.isDestroyed) {
                    pending.myEphemeralPrivateKey.destroy()
                }
            } catch (e: javax.security.auth.DestroyFailedException) {
                MeshLogger.d(TAG, "JVM Limitation: Secure erasure of ephemeral private key not supported by provider.")
            } catch (e: UnsupportedOperationException) {
                MeshLogger.d(TAG, "JVM Limitation: Secure erasure of ephemeral private key not supported by provider.")
            }
        }
    }
}
