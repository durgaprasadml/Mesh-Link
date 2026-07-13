package com.meshlink.domain.usecase.messaging

import com.meshlink.domain.model.Chat
import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetChatMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(chatId: String): Flow<List<Message>> = chatRepository.getMessagesForChat(chatId)
}

class GetBroadcastMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(): Flow<List<Message>> = chatRepository.getBroadcastMessages()
}

class GetAllChatsUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(): Flow<List<Chat>> = chatRepository.getAllChats()
}

class DeleteMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(messageIds: List<String>) = chatRepository.deleteMessages(messageIds)
}

class DeleteChatUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(chatId: String) = chatRepository.deleteChat(chatId)
}

class MarkChatAsReadUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(chatId: String) = chatRepository.markChatAsRead(chatId)
}

class GetMessageUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Message? = chatRepository.getMessageByUuid(messageId)
}
