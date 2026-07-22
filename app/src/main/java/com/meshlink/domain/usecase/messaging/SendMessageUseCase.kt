package com.meshlink.domain.usecase.messaging

import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.domain.repository.ChatRepository
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val meshRepository: MeshRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        targetMeshId: String, 
        messageText: String, 
        chatName: String
    ) {
        val user = userRepository.getLocalUser() ?: return
        val myMeshId = user.meshId

        val normalizedChatId = meshRepository.resolveChatId(targetMeshId)

        val messageId = UUID.randomUUID().toString()
        val message = Message(
            messageId = messageId,
            chatId = normalizedChatId,
            text = messageText,
            senderId = myMeshId,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = DeliveryStatus.PENDING,
            messageType = MessageType.TEXT
        )
        
        chatRepository.saveMessage(message, chatName)
        meshRepository.sendMessage(targetMeshId, message, chatName)
    }
}
