package com.meshlink.database.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.meshlink.util.RoomTestDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ChatDaoTest {

    private lateinit var database: MeshDatabase
    private lateinit var chatDao: ChatDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = RoomTestDatabase.createInMemoryDb(context)
        chatDao = database.chatDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insertChat and getChatById`() = runTest {
        val chat = ChatEntity("chat_1", "General", "Hello", 1000L, 0)
        chatDao.insertChat(chat)
        
        val retrieved = chatDao.getChatById("chat_1")
        assertEquals(chat, retrieved)
    }

    @Test
    fun `insertMessage and getMessageByUuid`() = runTest {
        val message = MessageEntity(
            messageId = "msg_1",
            chatId = "chat_1",
            senderId = "user_1",
            text = "Hello",
            timestamp = 1000L,
            isFromMe = true,
            status = DeliveryStatus.SENT
        )
        chatDao.insertMessage(message)
        
        val retrieved = chatDao.getMessageByUuid("msg_1")
        assertNotNull(retrieved)
        assertEquals("msg_1", retrieved?.messageId)
    }

    @Test
    fun `insertMessageAndUpdateChat creates new chat if it does not exist`() = runTest {
        val message = MessageEntity(
            messageId = "msg_1",
            chatId = "chat_1",
            senderId = "user_2",
            text = "Hello",
            timestamp = 1000L,
            isFromMe = false,
            status = DeliveryStatus.SENT
        )
        
        chatDao.insertMessageAndUpdateChat(message, "Remote User")
        
        val chat = chatDao.getChatById("chat_1")
        assertNotNull(chat)
        assertEquals("Hello", chat?.lastMessage)
        assertEquals(1, chat?.unreadCount)
    }

    @Test
    fun `insertMessageAndUpdateChat updates existing chat`() = runTest {
        chatDao.insertChat(ChatEntity("chat_1", "General", "Old", 500L, 0))
        
        val message = MessageEntity(
            messageId = "msg_1",
            chatId = "chat_1",
            senderId = "user_2",
            text = "New",
            timestamp = 1000L,
            isFromMe = false,
            status = DeliveryStatus.SENT
        )
        
        chatDao.insertMessageAndUpdateChat(message, "General")
        
        val chat = chatDao.getChatById("chat_1")
        assertNotNull(chat)
        assertEquals("New", chat?.lastMessage)
        assertEquals(1, chat?.unreadCount)
    }

    @Test
    fun `insertMessageAndUpdateChat updates media placeholder to completed even if status was altered`() = runTest {
        // 1. Initial placeholder inserted
        val placeholder = MessageEntity(
            messageId = "voice_transfer_1",
            chatId = "peer_b",
            senderId = "peer_b",
            text = "🎤 Receiving Voice Note...",
            timestamp = 1000L,
            isFromMe = false,
            status = DeliveryStatus.QUEUED,
            messageType = MessageType.VOICE,
            mediaPath = null
        )
        chatDao.insertMessageAndUpdateChat(placeholder, "Peer B")

        // 2. Simulate transfer state callback setting status to SENDING
        chatDao.updateMessageStatus("voice_transfer_1", DeliveryStatus.SENDING)

        // 3. Completed media arrives with real filePath and duration
        val completed = MessageEntity(
            messageId = "voice_transfer_1",
            chatId = "peer_b",
            senderId = "peer_b",
            text = "🎤 Voice Note",
            timestamp = 2000L,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.VOICE,
            mediaPath = "/data/user/0/com.meshlink/files/mesh_media/voice_test.amr",
            mediaDurationMs = 3500L,
            mediaSize = 14200L,
            mimeType = "audio/amr-wb"
        )
        chatDao.insertMessageAndUpdateChat(completed, "Peer B")

        val retrieved = chatDao.getMessageByUuid("voice_transfer_1")
        assertNotNull(retrieved)
        assertEquals(DeliveryStatus.DELIVERED, retrieved?.status)
        assertEquals("/data/user/0/com.meshlink/files/mesh_media/voice_test.amr", retrieved?.mediaPath)
        assertEquals(3500L, retrieved?.mediaDurationMs)
        assertEquals(14200L, retrieved?.mediaSize)
        assertEquals("audio/amr-wb", retrieved?.mimeType)
        assertEquals("🎤 Voice Note", retrieved?.text)
    }

    @Test
    fun `updateMessageStatus updates status correctly`() = runTest {
        val message = MessageEntity(
            messageId = "msg_1", chatId = "chat_1", senderId = "user_1", text = "Hello",
            timestamp = 1000L, isFromMe = true, status = DeliveryStatus.SENT
        )
        chatDao.insertMessage(message)
        
        chatDao.updateMessageStatus("msg_1", DeliveryStatus.DELIVERED)
        
        val retrieved = chatDao.getMessageByUuid("msg_1")
        assertEquals(DeliveryStatus.DELIVERED, retrieved?.status)
    }

    @Test
    fun `deleteChat removes chat and associated messages`() = runTest {
        chatDao.insertChat(ChatEntity("chat_1", "General", "Old", 500L, 0))
        chatDao.insertMessage(MessageEntity(messageId = "msg_1", chatId = "chat_1", senderId = "user_1", text = "Hello", timestamp = 1000L, isFromMe = true, status = DeliveryStatus.SENT))
        chatDao.insertMessage(MessageEntity(messageId = "msg_2", chatId = "chat_1", senderId = "user_1", text = "World", timestamp = 2000L, isFromMe = true, status = DeliveryStatus.SENT))
        
        chatDao.deleteChat("chat_1")
        
        assertNull(chatDao.getChatById("chat_1"))
        assertNull(chatDao.getMessageByUuid("msg_1"))
        assertNull(chatDao.getMessageByUuid("msg_2"))
    }

    @Test
    fun `getAllChats flow emits updates`() = runTest {
        chatDao.getAllChats().test {
            assertEquals(emptyList<ChatEntity>(), awaitItem())
            
            val chat = ChatEntity("chat_1", "General", "Hello", 1000L, 0)
            chatDao.insertChat(chat)
            
            assertEquals(listOf(chat), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getMessagesForChat flow emits updates`() = runTest {
        chatDao.getMessagesForChat("chat_1").test {
            assertEquals(emptyList<MessageEntity>(), awaitItem())
            
            val message = MessageEntity(messageId = "msg_1", chatId = "chat_1", senderId = "user_1", text = "Hello", timestamp = 1000L, isFromMe = true, status = DeliveryStatus.SENT)
            chatDao.insertMessage(message)
            
            val messages = awaitItem()
            assertEquals(1, messages.size)
            assertEquals("msg_1", messages[0].messageId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
