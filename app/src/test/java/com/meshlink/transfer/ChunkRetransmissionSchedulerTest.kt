package com.meshlink.transfer

import com.meshlink.routing.engine.TransportDiagnostics
import com.meshlink.routing.engine.TransportMetrics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ChunkRetransmissionSchedulerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var config: TransferConfiguration
    private lateinit var chunkManager: ChunkManager
    private lateinit var runtimeStateRegistry: TransferRuntimeStateRegistry
    private lateinit var slidingWindowManager: SlidingWindowManager
    private lateinit var workerPool: ParallelTransferWorkerPool
    private lateinit var chunkDispatcher: ChunkDispatcher
    private lateinit var metrics: TransportMetrics
    private lateinit var diagnostics: TransportDiagnostics
    private lateinit var retransmissionScheduler: ChunkRetransmissionScheduler

    @Before
    fun setUp() {
        config = TransferConfiguration().apply {
            wifiAckTimeoutMs = 100L // Fast timeout for testing
            retryLimit = 2
        }
        chunkManager = ChunkManager()
        runtimeStateRegistry = TransferRuntimeStateRegistry()
        slidingWindowManager = SlidingWindowManager(config, runtimeStateRegistry)
        metrics = TransportMetrics()
        diagnostics = TransportDiagnostics()
    }

    @After
    fun tearDown() {
        if (::retransmissionScheduler.isInitialized) {
            retransmissionScheduler.clearAll()
        }
        if (::workerPool.isInitialized) {
            workerPool.stopWorkers()
        }
    }

    @Test
    fun `retransmissionScheduler starts and stops monitoring cleanly`() = runTest(testDispatcher) {
        workerPool = ParallelTransferWorkerPool(config, testDispatcher, this)
        chunkDispatcher = ChunkDispatcher(
            chunkManager = chunkManager,
            slidingWindowManager = slidingWindowManager,
            workerPool = workerPool,
            runtimeStateRegistry = runtimeStateRegistry,
            diagnostics = diagnostics,
            ioDispatcher = testDispatcher
        )
        retransmissionScheduler = ChunkRetransmissionScheduler(
            config = config,
            slidingWindowManager = slidingWindowManager,
            runtimeStateRegistry = runtimeStateRegistry,
            chunkDispatcher = chunkDispatcher,
            metrics = metrics,
            diagnostics = diagnostics,
            ioDispatcher = testDispatcher,
            applicationScope = this
        )

        val testFile = tempFolder.newFile("sample.txt")
        testFile.writeBytes(ByteArray(500) { 0x11 })

        val session = TransferSession(
            transferId = "retransmit_test_1",
            senderId = "userA",
            targetId = "userB",
            fileName = "sample.txt",
            mimeType = "text/plain",
            totalBytes = 500L,
            totalChunks = 1,
            direction = TransferDirection.OUTGOING,
            state = TransferState.STREAMING
        )

        retransmissionScheduler.startMonitoring(
            session = session,
            file = testFile,
            onSendPacket = null,
            onFailure = {}
        )

        retransmissionScheduler.stopMonitoring("retransmit_test_1")
    }
}
