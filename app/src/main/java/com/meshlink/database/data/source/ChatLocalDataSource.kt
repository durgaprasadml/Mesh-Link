package com.meshlink.database.data.source

import com.meshlink.database.data.local.ChatEntity
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatLocalDataSource {
    fun getAllChats(): Flow<List<ChatEntity>>
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    fun getBroadcastMessages(): Flow<List<MessageEntity>>
    
    suspend fun getMessageByUuid(messageId: String): MessageEntity?
    suspend fun getChatById(chatId: String): ChatEntity?
    suspend fun getMessagesByStatus(status: DeliveryStatus): List<MessageEntity>
    suspend fun getUnreadIncomingMessages(chatId: String): List<String>
    suspend fun getMessagesListForChat(chatId: String): List<MessageEntity>
    suspend fun getMessagesByIds(messageIds: List<String>): List<MessageEntity>
    
    suspend fun insertChat(chat: ChatEntity)
    suspend fun insertMessage(message: MessageEntity)
    suspend fun insertMessageAndUpdateChat(message: MessageEntity, chatName: String)
    
    suspend fun updateMessageStatus(messageId: String, status: DeliveryStatus)
    suspend fun updateMediaMessage(messageId: String, status: DeliveryStatus, text: String, mediaPath: String?)
    
    suspend fun markChatAsRead(chatId: String)
    suspend fun markMessagesAsSeen(messageIds: List<String>)
    
    suspend fun deleteMessage(messageId: String)
    suspend fun deleteMessages(messageIds: List<String>)
    suspend fun deleteMessagesAndUpdateChat(messageIds: List<String>)
    suspend fun deleteMessagesForChat(chatId: String)
    suspend fun deleteChat(chatId: String)
}
