package com.meshlink.database.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DatabaseConcurrencyTest {

    private lateinit var database: MeshDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var auditLogDao: AuditLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeshDatabase::class.java).allowMainThreadQueries().build()
        chatDao = database.chatDao
        auditLogDao = database.auditLogDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `concurrent inserts into same table do not lose data or corrupt database`() = runTest {
        val insertCount = 100
        val jobs = (1..insertCount).map { i ->
            launch(Dispatchers.Default) {
                val log = AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    peerId = "peer_$i",
                    eventName = "concurrent_event",
                    severity = 1,
                    details = "details",
                    actionTaken = "none"
                )
                auditLogDao.insertAuditLog(log)
            }
        }
        
        jobs.joinAll()
        
        val count = auditLogDao.getAuditLogCount()
        assertEquals(insertCount, count)
    }

    @Test
    fun `concurrent transactions execute safely`() = runTest {
        val insertCount = 50
        val jobs = (1..insertCount).map { i ->
            launch(Dispatchers.Default) {
                val msg = MessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    chatId = "chat_1",
                    senderId = "sender",
                    text = "msg_$i",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    status = DeliveryStatus.SENT
                )
                chatDao.insertMessageAndUpdateChat(msg, "Chat 1")
            }
        }
        
        jobs.joinAll()
        
        val chat = chatDao.getChatById("chat_1")
        val messages = chatDao.getMessagesForChat("chat_1") // we cannot easily await this in test because it returns a flow
        // To verify we can just query directly
        val rawMessages = database.compileStatement("SELECT COUNT(*) FROM messages").simpleQueryForLong()
        
        assertEquals(50L, rawMessages)
        assertEquals(0, chat?.unreadCount) // isFromMe is true, so unread should be 0
    }
}
