package com.meshlink.messaging.data

import com.meshlink.database.data.source.ChatLocalDataSource
import com.meshlink.data.mapper.toDomain
import com.meshlink.data.mapper.toEntity
import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessagingRepositoryImpl @Inject constructor(
    private val chatLocalDataSource: ChatLocalDataSource
) : ChatRepository {

    override fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return chatLocalDataSource.getMessagesForChat(chatId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMessageByUuid(messageId: String): Message? {
        return chatLocalDataSource.getMessageByUuid(messageId)?.toDomain()
    }

    override fun getBroadcastMessages(): Flow<List<Message>> {
        return chatLocalDataSource.getBroadcastMessages().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllChats(): Flow<List<Chat>> {
        return chatLocalDataSource.getAllChats().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveMessage(message: Message, chatName: String?) {
        val entity = message.toEntity()
        if (chatName != null) {
            chatLocalDataSource.insertMessageAndUpdateChat(entity, chatName)
        } else {
            chatLocalDataSource.insertMessage(entity)
        }
    }

    override suspend fun updateMessageStatus(messageId: String, status: DeliveryStatus) {
        val deliveryStatus = try {
            com.meshlink.database.data.local.DeliveryStatus.valueOf(status.name)
        } catch (e: Exception) {
            return
        }
        chatLocalDataSource.updateMessageStatus(messageId, deliveryStatus)
    }

    override suspend fun deleteMessages(messageIds: List<String>) {
        val messages = chatLocalDataSource.getMessagesByIds(messageIds)
        deleteMediaForMessages(messages)
        chatLocalDataSource.deleteMessagesAndUpdateChat(messageIds)
    }

    override suspend fun deleteChat(chatId: String) {
        val messages = chatLocalDataSource.getMessagesListForChat(chatId)
        deleteMediaForMessages(messages)
        chatLocalDataSource.deleteChat(chatId)
    }

    private fun deleteMediaForMessages(messages: List<com.meshlink.database.data.local.MessageEntity>) {
        messages.forEach { msg ->
            msg.mediaPath?.let { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    com.meshlink.common.logger.MeshLogger.e("MessagingRepository", "Failed to delete media", e)
                }
            }
        }
    }

    override suspend fun markChatAsRead(chatId: String) {
        chatLocalDataSource.markChatAsRead(chatId)
    }
}

private fun com.meshlink.database.data.local.MessageEntity.toDomain() = Message(
    messageId = messageId,
    chatId = chatId,
    text = text,
    senderId = senderId,
    timestamp = timestamp,
    isFromMe = isFromMe,
    status = com.meshlink.domain.model.DeliveryStatus.valueOf(status.name),
    messageType = com.meshlink.domain.model.MessageType.valueOf(messageType.name),
    mediaPath = mediaPath,
    mimeType = mimeType,
    mediaWidth = mediaWidth,
    mediaHeight = mediaHeight,
    mediaSize = mediaSize,
    mediaChecksum = mediaChecksum,
    thumbnailBase64 = thumbnailBase64,
    mediaDurationMs = mediaDurationMs,
    latitude = latitude,
    longitude = longitude,
    batteryPercent = batteryPercent
)

private fun com.meshlink.database.data.local.ChatEntity.toDomain() = Chat(
    id = id,
    name = name,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount
)

private fun Message.toEntity() = com.meshlink.database.data.local.MessageEntity(
    messageId = messageId,
    chatId = chatId,
    text = text,
    senderId = senderId,
    timestamp = timestamp,
    isFromMe = isFromMe,
    status = com.meshlink.database.data.local.DeliveryStatus.valueOf(status.name),
    messageType = com.meshlink.database.data.local.MessageType.valueOf(messageType.name),
    mediaPath = mediaPath,
    mimeType = mimeType,
    mediaWidth = mediaWidth,
    mediaHeight = mediaHeight,
    mediaSize = mediaSize,
    mediaChecksum = mediaChecksum,
    thumbnailBase64 = thumbnailBase64,
    mediaDurationMs = mediaDurationMs,
    latitude = latitude,
    longitude = longitude,
    batteryPercent = batteryPercent
)
