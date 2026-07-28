package com.meshlink.ble.data.handlers

import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.transfer.TransferManager
import com.meshlink.transfer.TransferPriority
import com.meshlink.util.MeshIdNormalizer
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceMessageHandler @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val transferManager: TransferManager
) {
    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        val targetPeerId = MeshIdNormalizer.canonicalize(targetMeshId)

        val voiceFile = File(filePath)
        if (!voiceFile.exists()) return

        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId = messageId,
            chatId = targetPeerId,
            senderId = localPeerId,
            text = "🎤 Voice Note",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.PENDING,
            messageType = MessageType.VOICE,
            mediaPath = filePath,
            mediaDurationMs = durationMs
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)
        
        transferManager.sendFile(
            file = voiceFile,
            senderId = localPeerId,
            targetId = targetPeerId,
            transferId = messageId,
            priority = TransferPriority.HIGH
        )
    }
}
