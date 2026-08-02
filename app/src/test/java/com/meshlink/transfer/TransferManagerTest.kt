package com.meshlink.transfer

import android.content.Context
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.RouteType
import com.meshlink.routing.engine.IntelligentTransportManager
import com.meshlink.wifi.data.WifiSocketTransport
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TransferManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val scheduler = mockk<TransferScheduler>(relaxed = true)
    private val cache = mockk<TransferCache>(relaxed = true)
    private val chunkManager = ChunkManager()
    private val metaManager = mockk<FileMetadataManager>(relaxed = true)
    private val verifier = mockk<IntegrityVerifier>(relaxed = true)
    private val analytics = mockk<TransferAnalytics>(relaxed = true)
    private val intelligentTransportManager = mockk<IntelligentTransportManager>(relaxed = true)
    private val wifiSocketTransport = mockk<WifiSocketTransport>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val activeSessionsFlow = MutableStateFlow<List<TransferSession>>(emptyList())

    private lateinit var transferManager: TransferManager

    @Before
    fun setUp() {
        every { scheduler.activeSessions } returns activeSessionsFlow
        coEvery { cache.loadPersistedSessions() } returns emptyList()
        every { intelligentTransportManager.selectTransportForPayload(any(), any(), any(), any()) } returns RouteType.WIFI_DIRECT
        every { metaManager.getMimeTypeForFile(any()) } returns "image/jpeg"
        every { verifier.calculateFileChecksum(any()) } returns "dummy_sha256"

        transferManager = TransferManager(
            context = context,
            scheduler = scheduler,
            cache = cache,
            chunkManager = chunkManager,
            metaManager = metaManager,
            verifier = verifier,
            analytics = analytics,
            intelligentTransportManager = intelligentTransportManager,
            wifiSocketTransport = wifiSocketTransport,
            ioDispatcher = testDispatcher,
            applicationScope = testScope
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `sendFile selects Wi-Fi Direct for media files and creates TransferSession`() = runTest {
        val testFile = tempFolder.newFile("sample_image.jpg")
        testFile.writeBytes(ByteArray(1024 * 100) { 0x41 })

        val transferId = transferManager.sendFile(
            file = testFile,
            senderId = "userA",
            targetId = "userB",
            priority = TransferPriority.HIGH
        )

        assertNotNull(transferId)
        verify { scheduler.addSession(match { it.transportUsed == TransportType.WIFI_DIRECT && it.totalBytes == 102400L }) }
    }

    @Test
    fun `pauseTransfer and resumeTransfer update session state correctly`() = runTest {
        val session = TransferSession(
            transferId = "transfer_123",
            senderId = "userA",
            targetId = "userB",
            fileName = "voice.m4a",
            mimeType = "audio/m4a",
            totalBytes = 50000L,
            totalChunks = 5,
            direction = TransferDirection.OUTGOING,
            state = TransferState.STREAMING,
            transportUsed = TransportType.WIFI_DIRECT
        )

        every { scheduler.getSession("transfer_123") } returns session
        every { scheduler.updateSessionState("transfer_123", any()) } answers {
            session.state = secondArg()
        }

        transferManager.pauseTransfer("transfer_123")
        verify { scheduler.updateSessionState("transfer_123", TransferState.PAUSED) }

        transferManager.resumeTransfer("transfer_123")
        verify { scheduler.updateSessionState("transfer_123", TransferState.RESUMING) }
    }

    @Test
    fun `cancelTransfer cleans up session cache and sets CANCELLED state`() = runTest {
        val session = TransferSession(
            transferId = "transfer_cancel",
            senderId = "userA",
            targetId = "userB",
            fileName = "video.mp4",
            mimeType = "video/mp4",
            totalBytes = 2000000L,
            totalChunks = 20,
            direction = TransferDirection.OUTGOING,
            state = TransferState.STREAMING,
            transportUsed = TransportType.WIFI_DIRECT
        )

        every { scheduler.getSession("transfer_cancel") } returns session

        transferManager.cancelTransfer("transfer_cancel")

        verify { scheduler.updateSessionState("transfer_cancel", TransferState.CANCELLED) }
        coVerify { cache.cleanUpSession("transfer_cancel") }
    }

    @Test
    fun `64KB chunking calculates correct chunk count for 50MB and 100MB files`() {
        val fiftyMbSize = 50L * 1024L * 1024L
        val hundredMbSize = 100L * 1024L * 1024L

        val chunks50MB = chunkManager.getTotalChunks(fiftyMbSize, TransportType.WIFI_DIRECT)
        val chunks100MB = chunkManager.getTotalChunks(hundredMbSize, TransportType.WIFI_DIRECT)

        assertEquals(800, chunks50MB)
        assertEquals(1600, chunks100MB)
    }
}
