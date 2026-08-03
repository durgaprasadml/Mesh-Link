package com.meshlink.transfer

import com.meshlink.domain.model.MeshPacket
import com.meshlink.routing.engine.TransportDiagnostics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ChunkDispatcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var config: TransferConfiguration
    private lateinit var chunkManager: ChunkManager
    private lateinit var runtimeStateRegistry: TransferRuntimeStateRegistry
    private lateinit var slidingWindowManager: SlidingWindowManager
    private lateinit var workerPool: ParallelTransferWorkerPool
    private lateinit var diagnostics: TransportDiagnostics
    private lateinit var dispatcher: ChunkDispatcher

    @Before
    fun setUp() {
        config = TransferConfiguration()
        chunkManager = ChunkManager()
        runtimeStateRegistry = TransferRuntimeStateRegistry()
        slidingWindowManager = SlidingWindowManager(config, runtimeStateRegistry)
        diagnostics = TransportDiagnostics()
    }

    @After
    fun tearDown() {
        if (::workerPool.isInitialized) {
            workerPool.stopWorkers()
        }
    }

    @Test
    fun `dispatchAvailableChunks reads disk and dispatches packets within window`() = runTest(testDispatcher) {
        workerPool = ParallelTransferWorkerPool(config, testDispatcher, this)
        dispatcher = ChunkDispatcher(
            chunkManager = chunkManager,
            slidingWindowManager = slidingWindowManager,
            workerPool = workerPool,
            runtimeStateRegistry = runtimeStateRegistry,
            diagnostics = diagnostics,
            ioDispatcher = testDispatcher
        )

        val testFile = tempFolder.newFile("test_data.bin")
        testFile.writeBytes(ByteArray(1024 * 128) { 0x33 }) // 128 KB file

        val transferId = "dispatch_test_1"
        val totalChunks = chunkManager.getTotalChunks(testFile.length(), TransportType.WIFI_DIRECT)
        slidingWindowManager.initializeSessionWindow(transferId, TransportType.WIFI_DIRECT, totalChunks)

        val session = TransferSession(
            transferId = transferId,
            senderId = "userA",
            targetId = "userB",
            fileName = testFile.name,
            mimeType = "application/octet-stream",
            totalBytes = testFile.length(),
            totalChunks = totalChunks,
            direction = TransferDirection.OUTGOING,
            transportUsed = TransportType.WIFI_DIRECT
        )

        val dispatchedPackets = java.util.concurrent.CopyOnWriteArrayList<MeshPacket>()

        dispatcher.dispatchAvailableChunks(session, testFile) { packet ->
            dispatchedPackets.add(packet)
        }

        advanceUntilIdle()

        assertTrue("Dispatched packets should not be empty", dispatchedPackets.isNotEmpty())
        assertEquals(totalChunks, dispatchedPackets.size)
        assertEquals(0, dispatchedPackets.first().chunkIndex)
    }
}
