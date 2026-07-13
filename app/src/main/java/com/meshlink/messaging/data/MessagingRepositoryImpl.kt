package com.meshlink.messaging.data

import com.meshlink.database.data.source.ChatLocalDataSource
import com.meshlink.domain.mapper.toDomain
import com.meshlink.domain.mapper.toEntity
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
        chatLocalDataSource.deleteMessages(messageIds)
    }

    override suspend fun deleteChat(chatId: String) {
        chatLocalDataSource.deleteChat(chatId)
    }

    override suspend fun markChatAsRead(chatId: String) {
        chatLocalDataSource.markChatAsRead(chatId)
    }
}
