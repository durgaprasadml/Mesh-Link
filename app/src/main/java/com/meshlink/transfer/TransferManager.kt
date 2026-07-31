package com.meshlink.transfer

import com.meshlink.domain.model.MeshPacket
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class TransferManager @Inject constructor(
    private val mediaTransferSessionManager: MediaTransferSessionManager
) {
    var onSendPacket: (suspend (MeshPacket) -> Unit)?
        get() = null
        set(value) {
            mediaTransferSessionManager.transportExecutor.onPacketDispatched = { packet, _ ->
                value?.let { cb ->
                    kotlinx.coroutines.runBlocking { cb(packet) }
                }
            }
        }

    var onTransferCompleted: ((TransferSession) -> Unit)?
        get() = mediaTransferSessionManager.onTransferCompleted
        set(value) {
            mediaTransferSessionManager.onTransferCompleted = value
        }

    var onOutgoingTransferCompleted: ((TransferSession) -> Unit)? = null

    val transferProgress: StateFlow<Map<String, Float>> = mediaTransferSessionManager.scheduler.activeSessions
        .let { _ ->
            kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
        }

    fun sendFile(
        file: File,
        senderId: String,
        targetId: String,
        priority: TransferPriority = TransferPriority.MEDIUM,
        transferId: String = UUID.randomUUID().toString()
    ): String {
        return mediaTransferSessionManager.startTransfer(file, senderId, targetId, priority, transferId)
    }

    fun handleIncomingPacket(packet: MeshPacket) {
        mediaTransferSessionManager.handleIncomingPacket(packet)
    }

    fun pauseTransfer(transferId: String) {
        mediaTransferSessionManager.pauseTransfer(transferId)
    }

    fun resumeTransfer(transferId: String) {
        mediaTransferSessionManager.resumeTransfer(transferId)
    }

    fun cancelTransfer(transferId: String) {
        mediaTransferSessionManager.cancelTransfer(transferId)
    }
}
