package com.meshlink.database.data.source

import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.ChatEntity
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatLocalDataSourceImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatLocalDataSource {

    override fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = chatDao.getMessagesForChat(chatId)

    override fun getBroadcastMessages(): Flow<List<MessageEntity>> = chatDao.getBroadcastMessages()

    override suspend fun getMessageByUuid(messageId: String): MessageEntity? = chatDao.getMessageByUuid(messageId)

    override suspend fun getChatById(chatId: String): ChatEntity? = chatDao.getChatById(chatId)

    override suspend fun getMessagesByStatus(status: DeliveryStatus): List<MessageEntity> = chatDao.getMessagesByStatus(status)

    override suspend fun getUnreadIncomingMessages(chatId: String): List<String> = chatDao.getUnreadIncomingMessages(chatId)

    override suspend fun insertChat(chat: ChatEntity) {
        chatDao.insertChat(chat)
    }

    override suspend fun insertMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
    }

    override suspend fun insertMessageAndUpdateChat(message: MessageEntity, chatName: String) {
        chatDao.insertMessageAndUpdateChat(message, chatName)
    }

    override suspend fun updateMessageStatus(messageId: String, status: DeliveryStatus) {
        chatDao.updateMessageStatus(messageId, status)
    }

    override suspend fun updateMediaMessage(messageId: String, status: DeliveryStatus, text: String, mediaPath: String?) {
        chatDao.updateMediaMessage(messageId, status, text, mediaPath)
    }

    override suspend fun markChatAsRead(chatId: String) {
        chatDao.markChatAsRead(chatId)
    }

    override suspend fun markMessagesAsSeen(messageIds: List<String>) {
        chatDao.markMessagesAsSeen(messageIds)
    }

    override suspend fun deleteMessage(messageId: String) {
        chatDao.deleteMessage(messageId)
    }

    override suspend fun deleteMessages(messageIds: List<String>) {
        chatDao.deleteMessages(messageIds)
    }

    override suspend fun deleteMessagesForChat(chatId: String) {
        chatDao.deleteMessagesForChat(chatId)
    }

    override suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
    }
}
