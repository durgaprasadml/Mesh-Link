package com.meshlink.database.data.local

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

    @Query("UPDATE messages SET status = :newStatus WHERE messageId = :messageId AND status IN (:allowedPreviousStatuses)")
    suspend fun updateMessageStatusConditional(messageId: String, newStatus: DeliveryStatus, allowedPreviousStatuses: List<DeliveryStatus>): Int

    @Query("SELECT * FROM messages WHERE status = :status")
    suspend fun getMessagesByStatus(status: DeliveryStatus): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE status IN (:statuses)")
    suspend fun getMessagesByStatuses(statuses: List<DeliveryStatus>): List<MessageEntity>

    @Query("UPDATE messages SET status = :status, text = :text, mediaPath = :mediaPath, mediaDurationMs = COALESCE(:mediaDurationMs, mediaDurationMs), mimeType = COALESCE(:mimeType, mimeType), mediaSize = COALESCE(:mediaSize, mediaSize), latitude = COALESCE(:latitude, latitude), longitude = COALESCE(:longitude, longitude), batteryPercent = COALESCE(:batteryPercent, batteryPercent) WHERE messageId = :messageId")
    suspend fun updateMediaMessage(
        messageId: String,
        status: DeliveryStatus,
        text: String,
        mediaPath: String?,
        mediaDurationMs: Long? = null,
        mimeType: String? = null,
        mediaSize: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        batteryPercent: Int? = null
    )

    @Query("UPDATE messages SET thumbnailBase64 = :thumbnailBase64 WHERE messageId = :messageId")
    suspend fun updateMessageThumbnail(messageId: String, thumbnailBase64: String?)

    @Transaction
    suspend fun updateSosMediaAttachment(
        sosEventId: String,
        frontPath: String? = null,
        rearPath: String? = null,
        senderId: String? = null,
        chatId: String? = null,
        chatName: String? = null
    ): MessageEntity {
        val existing = getMessageByUuid(sosEventId)
            ?: getMessageByUuid("sos_$sosEventId")
            ?: if (sosEventId.startsWith("sos_")) getMessageByUuid(sosEventId.removePrefix("sos_")) else null
        val currentJson = try {
            if (existing?.mediaPath?.startsWith("{") == true) {
                org.json.JSONObject(existing.mediaPath)
            } else {
                org.json.JSONObject()
            }
        } catch (_: Exception) {
            org.json.JSONObject()
        }

        if (frontPath != null) {
            currentJson.put("front", frontPath)
        }
        if (rearPath != null) {
            currentJson.put("rear", rearPath)
        }
        val mergedPath = currentJson.toString()

        if (existing != null) {
            updateMediaMessage(
                messageId = existing.messageId,
                status = DeliveryStatus.DELIVERED,
                text = existing.text,
                mediaPath = mergedPath,
                mediaDurationMs = existing.mediaDurationMs,
                mimeType = existing.mimeType,
                mediaSize = existing.mediaSize,
                latitude = existing.latitude,
                longitude = existing.longitude,
                batteryPercent = existing.batteryPercent
            )
            val updated = existing.copy(
                mediaPath = mergedPath,
                status = DeliveryStatus.DELIVERED
            )
            val chat = getChatById(existing.chatId)
            if (chat != null) {
                val updatedName = if (!chatName.isNullOrBlank() && chatName != existing.chatId) chatName else chat.name
                insertChat(chat.copy(name = updatedName, lastMessage = existing.text, lastMessageAt = existing.timestamp))
            }
            return updated
        } else {
            val resolvedChatId = chatId ?: senderId ?: "BROADCAST"
            val resolvedSenderId = senderId ?: "UNKNOWN"
            val resolvedChatName = if (!chatName.isNullOrBlank()) chatName else "🚨 $resolvedChatId"
            val newMessage = MessageEntity(
                messageId = sosEventId,
                chatId = resolvedChatId,
                senderId = resolvedSenderId,
                text = "🚨 SOS EMERGENCY from $resolvedChatName",
                timestamp = System.currentTimeMillis(),
                isFromMe = false,
                status = DeliveryStatus.DELIVERED,
                messageType = MessageType.SOS,
                mediaPath = mergedPath
            )
            insertMessageAndUpdateChat(newMessage, resolvedChatName)
            return newMessage
        }
    }

    @Transaction
    suspend fun insertMessageAndUpdateChat(message: MessageEntity, chatName: String) {
        val existing = getMessageByUuid(message.messageId)
        if (existing != null) {
            if (message.messageType == MessageType.SOS) {
                val mergedPath = mergeSosMediaPath(existing.mediaPath, message.mediaPath)
                val resolvedText = if (message.text.contains("Lat:") || !existing.text.contains("Lat:")) message.text else existing.text
                updateMediaMessage(
                    messageId = message.messageId,
                    status = message.status,
                    text = resolvedText,
                    mediaPath = mergedPath,
                    mediaDurationMs = message.mediaDurationMs ?: existing.mediaDurationMs,
                    mimeType = message.mimeType ?: existing.mimeType,
                    mediaSize = message.mediaSize ?: existing.mediaSize,
                    latitude = message.latitude ?: existing.latitude,
                    longitude = message.longitude ?: existing.longitude,
                    batteryPercent = message.batteryPercent ?: existing.batteryPercent
                )
                val chat = getChatById(message.chatId)
                if (chat != null) {
                    val updatedName = if (chatName.isNotBlank() && chatName != message.chatId) chatName else chat.name
                    insertChat(chat.copy(
                        name = updatedName,
                        lastMessage = resolvedText,
                        lastMessageAt = message.timestamp
                    ))
                }
                return
            }

            if (message.mediaPath != null && (existing.mediaPath == null || existing.mediaPath != message.mediaPath || existing.status != message.status)) {
                // Update placeholder or incomplete media message to completed
                updateMediaMessage(
                    messageId = message.messageId,
                    status = message.status,
                    text = message.text,
                    mediaPath = message.mediaPath,
                    mediaDurationMs = message.mediaDurationMs,
                    mimeType = message.mimeType,
                    mediaSize = message.mediaSize,
                    latitude = message.latitude,
                    longitude = message.longitude,
                    batteryPercent = message.batteryPercent
                )
                val chat = getChatById(message.chatId)
                if (chat != null) {
                    val updatedName = if (chatName.isNotBlank() && chatName != message.chatId) chatName else chat.name
                    insertChat(chat.copy(
                        name = updatedName,
                        lastMessage = message.text,
                        lastMessageAt = message.timestamp
                    ))
                }
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
            val updatedName = if (chatName.isNotBlank() && chatName != message.chatId) chatName else chat.name
            chat = chat.copy(
                name = updatedName,
                lastMessage = message.text,
                lastMessageAt = message.timestamp,
                unreadCount = if (message.isFromMe) chat.unreadCount else chat.unreadCount + 1
            )
        }
        insertChat(chat)
    }

    companion object {
        fun mergeSosMediaPath(existingPath: String?, incomingPath: String?): String? {
            if (existingPath.isNullOrBlank()) return incomingPath
            if (incomingPath.isNullOrBlank()) return existingPath
            return try {
                val existingObj = if (existingPath.startsWith("{")) org.json.JSONObject(existingPath) else org.json.JSONObject()
                val incomingObj = if (incomingPath.startsWith("{")) org.json.JSONObject(incomingPath) else org.json.JSONObject()
                val merged = org.json.JSONObject()

                val eKeys = existingObj.keys()
                while (eKeys.hasNext()) {
                    val k = eKeys.next()
                    val v = existingObj.optString(k)
                    if (v.isNotBlank()) merged.put(k, v)
                }

                val iKeys = incomingObj.keys()
                while (iKeys.hasNext()) {
                    val k = iKeys.next()
                    val v = incomingObj.optString(k)
                    if (v.isNotBlank()) merged.put(k, v)
                }

                merged.toString()
            } catch (_: Exception) {
                incomingPath ?: existingPath
            }
        }
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
    @Query("SELECT * FROM messages WHERE chatId = 'BROADCAST' ORDER BY timestamp ASC")
    fun getBroadcastMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId")
    suspend fun getMessagesListForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE messageId IN (:messageIds)")
    suspend fun getMessagesByIds(messageIds: List<String>): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): MessageEntity?

    @Query("UPDATE chats SET lastMessage = :text, lastMessageAt = :timestamp WHERE id = :chatId")
    suspend fun updateChatLastMessage(chatId: String, text: String?, timestamp: Long)

    @Transaction
    suspend fun deleteMessagesAndUpdateChat(messageIds: List<String>) {
        val messages = getMessagesByIds(messageIds)
        val chatIds = messages.map { it.chatId }.distinct()
        
        deleteMessages(messageIds)

        for (chatId in chatIds) {
            val lastMsg = getLastMessageForChat(chatId)
            if (lastMsg != null) {
                updateChatLastMessage(chatId, lastMsg.text, lastMsg.timestamp)
            } else {
                updateChatLastMessage(chatId, null, 0L)
            }
        }
    }
}
