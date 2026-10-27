package com.meshlink.ble.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meshlink.ble.data.handlers.MediaMessageHandler
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.DeliveryStatus
import com.meshlink.database.data.local.MessageEntity
import com.meshlink.database.data.local.MessageType
import com.meshlink.database.data.local.UserDao
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.transfer.FileMetadataManager
import com.meshlink.transfer.TransferManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SosEmergencyTransferTest {

    private lateinit var database: com.meshlink.database.data.local.MeshDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var userDao: UserDao
    private val transferManager = mockk<TransferManager>(relaxed = true)
    private val metaManager = mockk<FileMetadataManager>(relaxed = true)
    private lateinit var context: Context
    private val userRepository = mockk<com.meshlink.domain.repository.UserRepository>(relaxed = true)
    private val packetDispatcher = mockk<com.meshlink.ble.api.PacketDispatcher>(relaxed = true)

    private lateinit var mediaMessageHandler: MediaMessageHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        mockkObject(com.meshlink.util.NotificationHelper)
        every { com.meshlink.util.NotificationHelper.showMessageNotification(any(), any(), any(), any()) } just Runs

        database = com.meshlink.util.RoomTestDatabase.createInMemoryDb(context)
        chatDao = database.chatDao
        userDao = database.userDao
        mediaMessageHandler = MediaMessageHandler(
            context = context,
            userRepository = userRepository,
            chatDao = chatDao,
            userDao = userDao,
            transferManager = transferManager,
            packetDispatcher = packetDispatcher,
            metaManager = metaManager
        )
    }

    @org.junit.After
    fun teardown() {
        unmockkObject(com.meshlink.util.NotificationHelper)
        database.close()
    }

    // TEST A: SOS WITH TWO MEDIA ATTACHMENTS (ONE SOS EVENT -> FRONT + REAR)
    @Test
    fun `testA_oneSosEventWithIndependentFrontAndRearAttachments`() {
        val sosEventId = UUID.randomUUID().toString()
        val frontTransferId = "${sosEventId}_front_PEER1"
        val rearTransferId = "${sosEventId}_rear_PEER1"

        val frontInfo = mediaMessageHandler.parseSosMediaInfo(frontTransferId)
        val rearInfo = mediaMessageHandler.parseSosMediaInfo(rearTransferId)

        assertTrue(frontInfo.isSos)
        assertTrue(frontInfo.isFront)
        assertFalse(frontInfo.isRear)
        assertEquals(sosEventId, frontInfo.sosEventId)

        assertTrue(rearInfo.isSos)
        assertFalse(rearInfo.isFront)
        assertTrue(rearInfo.isRear)
        assertEquals(sosEventId, rearInfo.sosEventId)

        // Both point to the exact same parent SOS event
        assertEquals(frontInfo.sosEventId, rearInfo.sosEventId)
    }

    // TEST B: FRONT AND REAR IDENTITY DISTINCTION
    @Test
    fun `testB_frontAndRearHaveDistinctIdentitiesAndReferenceSameSosEvent`() {
        val sosEventId = "sos_alert_999"
        val peerId = "PEER_B"
        val frontMediaId = "${sosEventId}_front_${peerId}"
        val rearMediaId = "${sosEventId}_rear_${peerId}"

        assertNotEquals(frontMediaId, rearMediaId)

        val frontInfo = mediaMessageHandler.parseSosMediaInfo(frontMediaId)
        val rearInfo = mediaMessageHandler.parseSosMediaInfo(rearMediaId)

        assertEquals("sos_alert_999", frontInfo.sosEventId)
        assertEquals("sos_alert_999", rearInfo.sosEventId)
    }

    // TEST C & D: COMPLETION CORRELATION AND NO OVERWRITE
    @Test
    fun `testC_and_D_completionCorrelationUpdatesCorrectAttachmentWithoutOverwrite`() = runTest {
        val sosEventId = "sos_uuid_101"
        val frontTransferId = "${sosEventId}_front_PEER_ALPHA"
        val rearTransferId = "${sosEventId}_rear_PEER_ALPHA"
        val frontLocalPath = "/local/cache/mesh_media/sos_${sosEventId}_front.jpg"
        val rearLocalPath = "/local/cache/mesh_media/sos_${sosEventId}_rear.jpg"

        // 1. Front completes
        mediaMessageHandler.receiveMediaMessage(
            completedTransferId = frontTransferId,
            completedFilePath = frontLocalPath,
            completedMimeType = "image/jpeg",
            completedSenderId = "PEER_ALPHA"
        )

        val afterFront = chatDao.getMessageByUuid(sosEventId)
        assertNotNull(afterFront)
        assertEquals(MessageType.SOS, afterFront?.messageType)
        val frontJson = JSONObject(afterFront?.mediaPath ?: "{}")
        assertEquals(frontLocalPath, frontJson.getString("front"))
        assertFalse(frontJson.has("rear"))

        // 2. Rear completes
        mediaMessageHandler.receiveMediaMessage(
            completedTransferId = rearTransferId,
            completedFilePath = rearLocalPath,
            completedMimeType = "image/jpeg",
            completedSenderId = "PEER_ALPHA"
        )

        val afterBoth = chatDao.getMessageByUuid(sosEventId)
        assertNotNull(afterBoth)
        assertEquals(MessageType.SOS, afterBoth?.messageType)
        val bothJson = JSONObject(afterBoth?.mediaPath ?: "{}")
        assertEquals(frontLocalPath, bothJson.getString("front"))
        assertEquals(rearLocalPath, bothJson.getString("rear"))
    }

    // TEST E: DUPLICATE SUPPRESSION - NO STANDALONE PLACEHOLDERS FOR SOS MEDIA
    @Test
    fun `testE_suppressStandalonePlaceholdersForSosMediaPacketsAndStreams`() = runTest {
        val sosEventId = "sos_evt_555"

        // 1. Packet with transferId having _front
        val packetFront = MeshPacket(
            packetId = "pkt-1",
            senderId = "PEER_X",
            targetId = "LOCAL",
            payload = "{}",
            type = PacketType.MEDIA_META,
            transferId = "${sosEventId}_front_PEER_X"
        )
        mediaMessageHandler.insertPlaceholderIncomingMedia(packetFront)

        // 2. Stream with fileName having _rear
        mediaMessageHandler.insertPlaceholderForStream(
            transferId = "stream-123",
            senderId = "PEER_X",
            fileName = "sos_${sosEventId}_rear.jpg",
            mimeType = "image/jpeg",
            totalBytes = 250000L
        )

        // Verify that NO standalone placeholder was inserted to chatDao
        assertNull(chatDao.getMessageByUuid("${sosEventId}_front_PEER_X"))
        assertNull(chatDao.getMessageByUuid("stream-123"))
    }

    // TEST F: STREAMING FALLBACK SOS EVENT ID EXTRACTION
    @Test
    fun `testF_extractSosEventIdFromFilenameWhenTransferIdIsStreamKey`() {
        val sosEventId = "unique_emergency_777"
        val streamTransferId = "wifi_stream_job_99"
        val filePath = "/data/user/0/com.meshlink/files/mesh_media/sos_${sosEventId}_front.jpg"

        val info = mediaMessageHandler.parseSosMediaInfo(streamTransferId, filePath = filePath)
        assertTrue(info.isSos)
        assertTrue(info.isFront)
        assertEquals(sosEventId, info.sosEventId)
    }

    // TEST G: VIRTUAL IMAGE VIEWPORT MAPPING FOR FULLSCREEN VIEWER
    @Test
    fun `testG_virtualImageKeysForFullscreenViewerMatchMediaViewerExpectations`() {
        val sosEventId = "sos_evt_888"
        val frontPath = "/path/to/front.jpg"
        val rearPath = "/path/to/rear.jpg"

        val mediaPathJson = JSONObject().apply {
            put("front", frontPath)
            put("rear", rearPath)
        }.toString()

        val sosMessage = MessageEntity(
            messageId = sosEventId,
            chatId = "PEER_Y",
            senderId = "PEER_Y",
            text = "🚨 SOS EMERGENCY",
            timestamp = 123456L,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            mediaPath = mediaPathJson
        )

        // Simulate ChatDetailScreen virtual extraction
        val json = JSONObject(sosMessage.mediaPath!!)
        val virtualImages = listOf("front", "rear").mapNotNull { key ->
            val path = json.optString(key).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            sosMessage.copy(
                messageId = "${sosMessage.messageId}#$key",
                messageType = MessageType.IMAGE,
                mediaPath = path
            )
        }

        assertEquals(2, virtualImages.size)
        assertEquals("${sosEventId}#front", virtualImages[0].messageId)
        assertEquals(frontPath, virtualImages[0].mediaPath)
        assertEquals("${sosEventId}#rear", virtualImages[1].messageId)
        assertEquals(rearPath, virtualImages[1].mediaPath)
    }
}
