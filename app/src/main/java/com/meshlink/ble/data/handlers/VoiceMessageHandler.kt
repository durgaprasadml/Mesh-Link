package com.meshlink.ble.data.handlers

import com.meshlink.common.logger.MeshLogger
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
    companion object {
        private const val TAG = "VoiceMessageHandler"
        private const val MIN_DURATION_MS = 300L
    }

    suspend fun sendVoiceNote(targetMeshId: String, filePath: String, durationMs: Long, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        val targetPeerId = MeshIdNormalizer.canonicalize(targetMeshId)

        val voiceFile = File(filePath)
        val messageId = UUID.randomUUID().toString()

        MeshLogger.i("AUDIO_SEND_START", "[AUDIO_SEND_START] transferId=$messageId peerId=$targetPeerId filePath=$filePath durationMs=$durationMs")
        MeshLogger.i("AUDIO_FILE_CHECK", "[AUDIO_FILE_CHECK] transferId=$messageId exists=${voiceFile.exists()} canRead=${voiceFile.canRead()} length=${voiceFile.length()}B durationMs=$durationMs")

        if (!voiceFile.exists() || !voiceFile.canRead() || voiceFile.length() == 0L || durationMs < MIN_DURATION_MS) {
            val errReason = "Voice note validation failed (exists=${voiceFile.exists()}, canRead=${voiceFile.canRead()}, length=${voiceFile.length()}B, duration=${durationMs}ms)"
            MeshLogger.e(TAG, "[AUDIO_TRANSFER_FAILURE] transferId=$messageId error=$errReason")
            val failedMessage = MessageEntity(
                messageId = messageId,
                chatId = targetPeerId,
                senderId = localPeerId,
                text = "🎤 Voice Note (Failed)",
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = DeliveryStatus.FAILED,
                messageType = MessageType.VOICE,
                mediaPath = filePath,
                mediaDurationMs = durationMs
            )
            chatDao.insertMessageAndUpdateChat(failedMessage, chatName)
            return
        }

        MeshLogger.i("AUDIO_FILE_READY", "[AUDIO_FILE_READY] transferId=$messageId fileName=${voiceFile.name} fileSize=${voiceFile.length()}B durationMs=$durationMs")

        val message = MessageEntity(
            messageId = messageId,
            chatId = targetPeerId,
            senderId = localPeerId,
            text = "🎤 Voice Note",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.QUEUED,
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
