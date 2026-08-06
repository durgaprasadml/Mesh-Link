package com.meshlink.profile

import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.di.ApplicationScope
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketPriority
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.transfer.TransferManager
import com.meshlink.transfer.TransferPriority
import com.meshlink.util.MeshIdNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSyncManager @Inject constructor(
    private val userRepository: UserRepository,
    private val profilePhotoManager: ProfilePhotoManager,
    private val transferManager: TransferManager,
    private val packetDispatcher: PacketDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    companion object {
        private const val TAG = "ProfileSyncManager"
    }

    private val pendingRequests = ConcurrentHashMap<String, String>() // peerMeshId -> expectedHash

    init {
        // Register completion listener with TransferManager for incoming profile image transfers
        val originalCompletedListener = transferManager.onTransferCompleted
        transferManager.onTransferCompleted = { session ->
            originalCompletedListener?.invoke(session)

            if (session.mimeType == "image/webp" && session.filePath != null) {
                applicationScope.launch(Dispatchers.IO) {
                    val peerMeshId = MeshIdNormalizer.canonicalize(session.senderId)
                    val expectedHash = pendingRequests.remove(peerMeshId)
                    val file = File(session.filePath!!)

                    val result = profilePhotoManager.validateAndSavePeerPhoto(peerMeshId, file, expectedHash)
                    if (result != null) {
                        val (savedFile, hash) = result
                        userRepository.updateProfilePhoto(
                            meshId = peerMeshId,
                            photoPath = savedFile.absolutePath,
                            photoHash = hash,
                            version = System.currentTimeMillis(),
                            lastUpdated = System.currentTimeMillis()
                        )
                        MeshLogger.d(TAG, "Successfully synchronized profile picture for peer $peerMeshId")
                    } else {
                        MeshLogger.w(TAG, "Failed validating profile picture for peer $peerMeshId")
                    }
                }
            }
        }
    }

    /**
     * Called during KeyExchange/Handshake when peer's photo hash is discovered.
     */
    fun onPeerProfileHashDiscovered(peerMeshId: String, peerPhotoHash: String?) {
        if (peerPhotoHash.isNullOrBlank()) return
        val canonicalId = MeshIdNormalizer.canonicalize(peerMeshId)

        applicationScope.launch(Dispatchers.IO) {
            val user = userRepository.getUserProfile(canonicalId)
            val localFile = profilePhotoManager.getProfilePhotoFile(canonicalId)

            val currentHash = user?.profilePhotoHash ?: if (localFile.exists()) profilePhotoManager.computeSha256(localFile) else null

            if (currentHash != peerPhotoHash || !localFile.exists()) {
                MeshLogger.d(TAG, "Peer $canonicalId photo hash changed or missing (local: $currentHash, peer: $peerPhotoHash). Requesting picture...")
                requestProfilePhoto(canonicalId, peerPhotoHash)
            } else {
                MeshLogger.d(TAG, "Peer $canonicalId profile picture up to date (hash: $peerPhotoHash)")
            }
        }
    }

    /**
     * Sends a PROFILE_IMAGE_REQUEST packet to peer.
     */
    fun requestProfilePhoto(peerMeshId: String, expectedHash: String? = null) {
        val canonicalId = MeshIdNormalizer.canonicalize(peerMeshId)
        if (expectedHash != null) {
            pendingRequests[canonicalId] = expectedHash
        }

        applicationScope.launch(Dispatchers.IO) {
            val localUser = userRepository.getLocalUser() ?: return@launch
            val requestPacket = MeshPacket(
                senderId = MeshIdNormalizer.canonicalize(localUser.meshId),
                targetId = canonicalId,
                payload = "REQUEST_PROFILE_PHOTO",
                type = PacketType.PROFILE_IMAGE_REQUEST,
                priority = PacketPriority.LOW
            )
            packetDispatcher.dispatchSinglePacket(canonicalId, requestPacket)
            MeshLogger.d(TAG, "Dispatched PROFILE_IMAGE_REQUEST to $canonicalId")
        }
    }

    /**
     * Handles incoming PROFILE_IMAGE_REQUEST packet.
     */
    suspend fun handleProfileImageRequest(packet: MeshPacket) = withContext(Dispatchers.IO) {
        val localUser = userRepository.getLocalUser() ?: return@withContext
        val localMeshId = MeshIdNormalizer.canonicalize(localUser.meshId)
        val photoFile = profilePhotoManager.getProfilePhotoFile(localMeshId)

        if (!photoFile.exists() || photoFile.length() == 0L) {
            MeshLogger.d(TAG, "Local user has no profile photo to send to ${packet.senderId}")
            return@withContext
        }

        MeshLogger.d(TAG, "Sending profile photo to ${packet.senderId} via TransferManager...")
        transferManager.sendFile(
            file = photoFile,
            senderId = localMeshId,
            targetId = packet.senderId,
            priority = TransferPriority.BACKGROUND
        )
    }

    /**
     * Broadcasts profile photo change to nearby peers when user updates photo in Settings.
     */
    fun notifyProfilePhotoUpdated(newPhotoFile: File, newHash: String) {
        applicationScope.launch(Dispatchers.IO) {
            val localUser = userRepository.getLocalUser() ?: return@launch
            val localMeshId = MeshIdNormalizer.canonicalize(localUser.meshId)

            userRepository.updateProfilePhoto(
                meshId = localMeshId,
                photoPath = newPhotoFile.absolutePath,
                photoHash = newHash,
                version = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )

            // Broadcast PROFILE_IMAGE_RESPONSE / notification to nearby peers
            val updatePacket = MeshPacket(
                senderId = localMeshId,
                targetId = "BROADCAST",
                payload = "PROFILE_PHOTO_UPDATED|$newHash",
                type = PacketType.PROFILE_IMAGE_RESPONSE,
                priority = PacketPriority.LOW
            )
            packetDispatcher.dispatchSinglePacket("BROADCAST", updatePacket)
            MeshLogger.d(TAG, "Broadcasted profile photo update notification with hash $newHash")
        }
    }

    /**
     * Handles incoming PROFILE_IMAGE_RESPONSE update notification.
     */
    fun handleProfileImageResponse(packet: MeshPacket) {
        val payload = packet.payload
        if (payload.startsWith("PROFILE_PHOTO_UPDATED|")) {
            val peerHash = payload.substringAfter("PROFILE_PHOTO_UPDATED|").trim()
            if (peerHash.isNotBlank()) {
                onPeerProfileHashDiscovered(packet.senderId, peerHash)
            }
        }
    }
}
