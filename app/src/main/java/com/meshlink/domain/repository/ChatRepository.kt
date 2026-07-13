package com.meshlink.domain.repository

import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesForChat(chatId: String): Flow<List<Message>>
    suspend fun getMessageByUuid(messageId: String): Message?
    fun getBroadcastMessages(): Flow<List<Message>>
    fun getAllChats(): Flow<List<Chat>>
    
    suspend fun saveMessage(message: Message, chatName: String?)
    suspend fun updateMessageStatus(messageId: String, status: DeliveryStatus)
    suspend fun deleteMessages(messageIds: List<String>)
    suspend fun deleteChat(chatId: String)
    suspend fun markChatAsRead(chatId: String)
}
