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

        android.util.Log.d("[DIAG-Stage2]", "═══ SendMessageUseCase ═══")
        android.util.Log.d("[DIAG-Stage2]", "  RAW targetMeshId       : '$targetMeshId'")
        android.util.Log.d("[DIAG-Stage2]", "  RAW myMeshId           : '$myMeshId'")
        android.util.Log.d("[DIAG-Stage2]", "  resolveChatId(target)  : '$normalizedChatId'  [used as chatId in DB]")

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

        android.util.Log.d("[DIAG-Stage2]", "  message.chatId         : '${message.chatId}'")
        android.util.Log.d("[DIAG-Stage2]", "  message.senderId       : '${message.senderId}'")
        android.util.Log.d("[DIAG-Stage2]", "  message.messageId      : '${message.messageId.takeLast(6)}'")

        chatRepository.saveMessage(message, chatName)
        android.util.Log.d("[DIAG-Stage2]", "  chatRepository.saveMessage() called  ✓")
        meshRepository.sendMessage(targetMeshId, message)
        android.util.Log.d("[DIAG-Stage2]", "  meshRepository.sendMessage() called  ✓")
    }
}
