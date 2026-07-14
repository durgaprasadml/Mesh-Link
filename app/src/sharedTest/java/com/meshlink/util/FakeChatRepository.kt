package com.meshlink.util

import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeChatRepository : ChatRepository {
    private val messagesFlow = MutableStateFlow<List<Message>>(emptyList())
    private val chatsFlow = MutableStateFlow<List<Chat>>(emptyList())

    override fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messagesFlow.map { list -> list.filter { it.chatId == chatId } }
    }

    override suspend fun getMessageByUuid(messageId: String): Message? {
        return messagesFlow.value.find { it.messageId == messageId }
    }

    override fun getBroadcastMessages(): Flow<List<Message>> {
        return messagesFlow.map { list -> list.filter { it.chatId == "broadcast" } }
    }

    override fun getAllChats(): Flow<List<Chat>> {
        return chatsFlow
    }

    override suspend fun saveMessage(message: Message, chatName: String?) {
        val current = messagesFlow.value.toMutableList()
        current.add(message)
        messagesFlow.value = current
    }

    override suspend fun updateMessageStatus(messageId: String, status: DeliveryStatus) {
        val current = messagesFlow.value.map {
            if (it.messageId == messageId) it.copy(status = status) else it
        }
        messagesFlow.value = current
    }

    override suspend fun deleteMessages(messageIds: List<String>) {
        val current = messagesFlow.value.filterNot { messageIds.contains(it.messageId) }
        messagesFlow.value = current
    }

    override suspend fun deleteChat(chatId: String) {
        val currentChats = chatsFlow.value.filterNot { it.id == chatId }
        chatsFlow.value = currentChats
    }

    override suspend fun markChatAsRead(chatId: String) {
        val current = messagesFlow.value.map {
            if (it.chatId == chatId && it.status != DeliveryStatus.SEEN) {
                it.copy(status = DeliveryStatus.SEEN)
            } else {
                it
            }
        }
        messagesFlow.value = current
    }
}
