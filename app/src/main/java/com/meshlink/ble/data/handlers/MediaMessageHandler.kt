package com.meshlink.ble.data.handlers

import android.content.Context
import android.net.Uri
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.UserRepository
import com.meshlink.media.data.ImageCompressor
import com.meshlink.transfer.TransferManager
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaMessageHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val transferManager: TransferManager,
    private val packetDispatcher: PacketDispatcher
) {
    private val TAG = "MediaMessageHandler"

    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        val targetPeerId = MeshIdNormalizer.canonicalize(targetMeshId)

        val compressedBytes = withContext(Dispatchers.IO) {
            ImageCompressor.compress(context, imageUri)
        }
        if (compressedBytes == null) {
            MeshLogger.e(TAG, "sendImage: compression failed for $imageUri")
            return
        }
        MeshLogger.d(TAG, "sendImage: compressed to ${compressedBytes.size / 1000}KB")

        val thumbnailBase64 = withContext(Dispatchers.IO) {
            ImageCompressor.generateThumbnailBase64(context, imageUri)
        }

        if (imageUri.scheme == "content" && imageUri.authority?.contains("fileprovider") == true) {
            try {
                val tempFile = File(context.cacheDir, "images/${imageUri.lastPathSegment}")
                if (tempFile.exists()) tempFile.delete()
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Failed to delete temp camera file")
            }
        }

        val localFile = withContext(Dispatchers.IO) {
            val mediaDir = File(context.filesDir, "mesh_media")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            File(mediaDir, "img_${System.currentTimeMillis()}.jpg").apply {
                writeBytes(compressedBytes)
            }
        }

        val chatId = targetPeerId
        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            messageId       = messageId,
            chatId          = chatId,
            senderId        = localPeerId,
            text            = "📷 Image",
            timestamp       = System.currentTimeMillis(),
            isFromMe        = true,
            status          = DeliveryStatus.PENDING,
            messageType     = MessageType.IMAGE,
            mediaPath       = localFile.absolutePath,
            mimeType        = "image/jpeg",
            mediaSize       = localFile.length(),
            thumbnailBase64 = thumbnailBase64
        )
        chatDao.insertMessageAndUpdateChat(message, chatName)
        transferManager.sendFile(
            file = localFile,
            senderId = localPeerId,
            targetId = targetPeerId,
            transferId = messageId
        )
    }

    suspend fun receiveMediaMessage(completedTransferId: String, completedFilePath: String, completedMimeType: String, completedSenderId: String) {
        val isImage = completedMimeType.contains("image")
        val isVoice = completedMimeType.contains("audio")

        val messageType = when {
            isImage -> MessageType.IMAGE
            isVoice -> MessageType.VOICE
            else -> MessageType.TEXT
        }

        val previewText = when {
            isImage -> "📷 Image"
            isVoice -> "🎤 Voice Note"
            else -> "Unsupported File"
        }

        val chatId = MeshIdNormalizer.canonicalize(completedSenderId)
        val senderName = MeshIdNormalizer.canonicalize(completedSenderId)

        val message = MessageEntity(
            messageId = completedTransferId,
            chatId = chatId,
            senderId = completedSenderId,
            text = previewText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = messageType,
            mediaPath = completedFilePath
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
        
        userRepository.getLocalUser()?.let { user ->
            val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = completedSenderId,
                payload = completedTransferId,
                type = PacketType.DELIVERY_ACK,
                encrypted = false
            )
            packetDispatcher.dispatchSinglePacket(completedSenderId, ackPacket)
        }

        NotificationHelper.showMessageNotification(context, completedSenderId, senderName, previewText)
    }

    suspend fun insertPlaceholderIncomingMedia(packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        if (chatDao.getMessageByUuid(transferId) != null) return

        val isImage = packet.mimeType?.contains("image") == true
        val isVoice = packet.mimeType?.contains("audio") == true

        val messageType = when {
            isImage -> MessageType.IMAGE
            isVoice -> MessageType.VOICE
            else -> MessageType.TEXT
        }

        val previewText = when {
            isImage -> "📷 Receiving Image..."
            isVoice -> "🎤 Receiving Voice Note..."
            else -> "Receiving File..."
        }

        val chatId = MeshIdNormalizer.canonicalize(packet.senderId)
        val senderName = MeshIdNormalizer.canonicalize(packet.senderId)

        val message = MessageEntity(
            messageId = transferId,
            chatId = chatId,
            senderId = packet.senderId,
            text = previewText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.PENDING,
            messageType = messageType,
            mediaPath = null
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
    }
}
