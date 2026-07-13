package com.meshlink.data.media

import android.content.Context
import com.meshlink.data.ble.MeshPacket
import com.meshlink.data.ble.PacketType
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import android.util.Base64
import android.util.Log

@OptIn(ExperimentalCoroutinesApi::class)
class MediaTransferManagerTest {

    private lateinit var context: Context
    private lateinit var manager: MediaTransferManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        mockkStatic(Base64::class)
        every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
            val bytes = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
        every { Base64.decode(any<String>(), any()) } answers {
            val str = arg<String>(0)
            java.util.Base64.getDecoder().decode(str)
        }

        manager = MediaTransferManager(context)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `test chunk creation generates META and CHUNK packets`() {
        val data = ByteArray(500) { 1 } // 500 bytes of mock data
        val packets = manager.createChunkedPackets(
            data = data,
            senderId = "sender1",
            targetId = "target1",
            mimeType = "image/jpeg"
        )

        // 500 bytes Base64 -> ~668 characters. 300 per chunk -> 3 chunks
        // Packets: 1 META + 3 CHUNK = 4 total packets
        assertTrue(packets.isNotEmpty())
        assertEquals(PacketType.MEDIA_META, packets[0].type)
        assertEquals(PacketType.MEDIA_CHUNK, packets[1].type)
        assertEquals(3, packets[0].totalChunks)
    }

    @Test
    fun `test receive META packet initiates tracking`() {
        val transferId = "test-transfer"
        val metaPacket = MeshPacket(
            senderId = "sender1",
            targetId = "local_node",
            payload = "MEDIA:image/jpeg:fake_hash",
            type = PacketType.MEDIA_META,
            transferId = transferId,
            chunkIndex = 0,
            totalChunks = 2,
            mimeType = "image/jpeg"
        )
        
        val result = manager.handleIncomingMediaPacket(metaPacket)
        assertNull(result) // Still waiting for chunks
    }
}
