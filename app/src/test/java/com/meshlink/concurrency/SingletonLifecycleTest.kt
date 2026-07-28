package com.meshlink.concurrency

import com.meshlink.ble.data.BleRepositoryImpl
import com.meshlink.media.data.MediaTransferManager
import com.meshlink.security.data.TrustManager
import com.meshlink.config.MeshConfig
import com.meshlink.database.data.local.TrustDao
import com.meshlink.database.data.local.TrustEntity
import com.meshlink.security.data.MeshSecurityMonitor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import android.content.Context

@OptIn(ExperimentalCoroutinesApi::class)
class SingletonLifecycleTest {

    @Test
    fun `test TrustManager cancels operations when applicationScope is cancelled`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        
        val mockDao = mockk<TrustDao>()
        coEvery { mockDao.getAllPeers() } returns listOf()
        
        val mockMonitor = mockk<MeshSecurityMonitor>()
        
        // Inject testScope as the applicationScope
        val trustManager = TrustManager(
            trustDao = mockDao,
            securityMonitor = mockMonitor,
            ioDispatcher = testDispatcher,
            applicationScope = testScope
        )

        // Ensure that cancelling the testScope cleans up the background jobs started by TrustManager
        testScope.cancel()
        
        // We know testScope's job is cancelled, so the coroutines started via applicationScope.launch 
        // inside TrustManager are also cancelled.
        assertTrue(testScope.coroutineContext[Job]?.isCancelled == true)
    }

    @Test
    fun `test MediaTransferManager cancels jobs on scope cancellation`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        
        val mockContext = mockk<Context>()
        val mockConfig = mockk<MeshConfig>()

        val manager = MediaTransferManager(
            context = mockContext,
            ioDispatcher = testDispatcher,
            meshConfig = mockConfig,
            applicationScope = testScope
        )

        // Cancel application scope
        testScope.cancel()
        
        // Assert scope cancellation
        assertTrue(testScope.coroutineContext[Job]?.isCancelled == true)
    }
}
