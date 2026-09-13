package com.meshlink.ble.data.handlers

import android.content.Context
import android.net.Uri
import com.meshlink.ble.api.PacketDispatcher
import com.meshlink.common.logger.MeshLogger
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.database.data.local.UserDao
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
    private val userDao: UserDao,
    private val transferManager: TransferManager,
    private val packetDispatcher: PacketDispatcher,
    private val metaManager: com.meshlink.transfer.FileMetadataManager
) {
    private val TAG = "MediaMessageHandler"

    suspend fun sendImage(targetMeshId: String, imageUri: Uri, chatName: String) {
        val user = userRepository.getLocalUser() ?: return
        val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
        val targetPeerId = MeshIdNormalizer.canonicalize(targetMeshId)

        val thumbnailBase64 = withContext(Dispatchers.IO) {
            ImageCompressor.generateThumbnailBase64(context, imageUri)
        }

        val localFile = withContext(Dispatchers.IO) {
            var tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
            try {
                val compressedBytes = ImageCompressor.compress(context, imageUri)
                if (compressedBytes != null) {
                    val compressedFile = File(context.cacheDir, "comp_${System.currentTimeMillis()}.jpg")
                    compressedFile.writeBytes(compressedBytes)
                    compressedFile
                } else {
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to compress or cache image: ${e.message}")
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } finally {
                if (tempFile.exists()) tempFile.delete()
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
            status          = DeliveryStatus.QUEUED,
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
            transferId = messageId,
            thumbnailBase64 = thumbnailBase64
        )
    }

    suspend fun receiveMediaMessage(completedTransferId: String, completedFilePath: String, completedMimeType: String, completedSenderId: String) {
        val isSosFront = completedTransferId.contains("_front") || completedFilePath.contains("_front")
        val isSosRear = completedTransferId.contains("_rear") || completedFilePath.contains("_rear")
        val isSosMedia = isSosFront || isSosRear

        val existingUser = userDao.getUser(completedSenderId)
        val resolvedSenderName = when {
            existingUser != null && existingUser.name.isNotBlank() && existingUser.name != "Unknown User" -> existingUser.name
            else -> MeshIdNormalizer.canonicalize(completedSenderId)
        }

        val chatId = MeshIdNormalizer.canonicalize(completedSenderId)

        if (isSosMedia) {
            // Extract the base SOS event ID
            val rawId = completedTransferId.substringBefore("_front").substringBefore("_rear").removePrefix("sos_")
            val sosEventId = rawId.ifBlank { completedTransferId }

            val existingSos = chatDao.getMessageByUuid(sosEventId)
            val currentMediaJson = try {
                if (existingSos?.mediaPath?.startsWith("{") == true) {
                    org.json.JSONObject(existingSos.mediaPath)
                } else {
                    org.json.JSONObject()
                }
            } catch (_: Exception) {
                org.json.JSONObject()
            }

            if (isSosFront) {
                currentMediaJson.put("front", completedFilePath)
            }
            if (isSosRear) {
                currentMediaJson.put("rear", completedFilePath)
            }

            if (existingSos != null) {
                val updatedMessage = existingSos.copy(
                    mediaPath = currentMediaJson.toString(),
                    status = DeliveryStatus.DELIVERED
                )
                chatDao.insertMessageAndUpdateChat(updatedMessage, "🚨 $resolvedSenderName")
            } else {
                val placeholderMessage = MessageEntity(
                    messageId = sosEventId,
                    chatId = chatId,
                    senderId = completedSenderId,
                    text = "🚨 SOS EMERGENCY from $resolvedSenderName",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = false,
                    status = DeliveryStatus.DELIVERED,
                    messageType = MessageType.SOS,
                    mediaPath = currentMediaJson.toString()
                )
                chatDao.insertMessageAndUpdateChat(placeholderMessage, "🚨 $resolvedSenderName")
            }

            NotificationHelper.showMessageNotification(
                context,
                completedSenderId,
                "🚨 $resolvedSenderName",
                "🚨 SOS Emergency dual camera image received"
            )
        } else {
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
            chatDao.insertMessageAndUpdateChat(message, resolvedSenderName)

            NotificationHelper.showMessageNotification(context, completedSenderId, resolvedSenderName, previewText)
        }

        userRepository.getLocalUser()?.let { user ->
            val localPeerId = MeshIdNormalizer.canonicalize(user.meshId)
            val ackPacket = MeshPacket(
                senderId = localPeerId,
                targetId = completedSenderId,
                payload = completedTransferId,
                type = PacketType.DELIVERY_ACK,
                priority = com.meshlink.domain.model.PacketPriority.CRITICAL,
                encrypted = false
            )
            packetDispatcher.dispatchSinglePacket(completedSenderId, ackPacket)
        }
    }

    suspend fun insertPlaceholderIncomingMedia(packet: MeshPacket) {
        val transferId = packet.transferId ?: return
        val parsedMeta = metaManager.parseMetaPayload(packet.payload)

        val mime = parsedMeta?.mimeType ?: packet.mimeType
        val isImage = mime?.contains("image") == true
        val isVoice = mime?.contains("audio") == true

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

        val existingMessage = chatDao.getMessageByUuid(transferId)
        if (existingMessage != null) {
            if (parsedMeta?.thumbnailBase64 != null && existingMessage.thumbnailBase64 == null) {
                chatDao.updateMessageThumbnail(transferId, parsedMeta.thumbnailBase64)
            }
            return
        }

        val message = MessageEntity(
            messageId = transferId,
            chatId = chatId,
            senderId = packet.senderId,
            text = previewText,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.QUEUED,
            messageType = messageType,
            mediaPath = null,
            mimeType = mime,
            mediaSize = parsedMeta?.totalBytes,
            thumbnailBase64 = parsedMeta?.thumbnailBase64
        )
        chatDao.insertMessageAndUpdateChat(message, senderName)
    }
}
