package com.meshlink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageByUuid(messageId: String): MessageEntity?

    @Query("SELECT * FROM chats ORDER BY lastMessageAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: DeliveryStatus)

    @Query("SELECT * FROM messages WHERE status = :status")
    suspend fun getMessagesByStatus(status: DeliveryStatus): List<MessageEntity>

    @Query("UPDATE messages SET status = :status, text = :text, mediaPath = :mediaPath WHERE messageId = :messageId")
    suspend fun updateMediaMessage(messageId: String, status: DeliveryStatus, text: String, mediaPath: String?)

    @Transaction
    suspend fun insertMessageAndUpdateChat(message: MessageEntity, chatName: String) {
        val existing = getMessageByUuid(message.messageId)
        if (existing != null) {
            if (existing.status == DeliveryStatus.PENDING && existing.mediaPath == null) {
                // Update placeholder to completed
                updateMediaMessage(message.messageId, message.status, message.text, message.mediaPath)
            }
            return // Ignore duplicate
        }
        
        insertMessage(message)
        var chat = getChatById(message.chatId)
        if (chat == null) {
            chat = ChatEntity(
                id = message.chatId,
                name = chatName,
                lastMessage = message.text,
                lastMessageAt = message.timestamp,
                unreadCount = if (message.isFromMe) 0 else 1
            )
        } else {
            chat = chat.copy(
                lastMessage = message.text,
                lastMessageAt = message.timestamp,
                unreadCount = if (message.isFromMe) chat.unreadCount else chat.unreadCount + 1
            )
        }
        insertChat(chat)
    }

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: String)

    @Query("SELECT messageId FROM messages WHERE chatId = :chatId AND isFromMe = 0 AND status = 'DELIVERED'")
    suspend fun getUnreadIncomingMessages(chatId: String): List<String>

    @Query("UPDATE messages SET status = 'SEEN' WHERE messageId IN (:messageIds)")
    suspend fun markMessagesAsSeen(messageIds: List<String>)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE messageId IN (:messageIds)")
    suspend fun deleteMessages(messageIds: List<String>)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Transaction
    suspend fun deleteChat(chatId: String) {
        deleteMessagesForChat(chatId)
        deleteChatEntity(chatId)
    }

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatEntity(chatId: String)

    // FIX ERROR 3: Fetch all messages that were broadcast to all nodes
    @Query("SELECT * FROM messages WHERE chatId = 'BROADCAST' ORDER BY timestamp DESC")
    fun getBroadcastMessages(): Flow<List<MessageEntity>>
}
