package com.meshlink.ble.data

import com.meshlink.routing.data.MeshRouter
import com.meshlink.routing.engine.RoutingEngine
import com.meshlink.database.data.local.RelayDao
import com.meshlink.database.data.local.TrustDao
import com.meshlink.domain.repository.SettingsRepository
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field
import kotlinx.coroutines.flow.MutableSharedFlow
import io.mockk.every

@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrencyTests {

    private lateinit var meshRouter: MeshRouter
    private val routingEngine: RoutingEngine = mockk(relaxed = true)
    private val relayDao: RelayDao = mockk(relaxed = true)
    private val trustDao: TrustDao = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        val bleTransport: com.meshlink.ble.api.BleTransport = mockk(relaxed = true)
        every { bleTransport.incomingPackets } returns MutableSharedFlow()

        every { settingsRepository.advancedEncryptionEnforcement } returns kotlinx.coroutines.flow.flowOf(true)
        every { settingsRepository.isMeshRelayEnabled } returns kotlinx.coroutines.flow.flowOf(true)
        every { settingsRepository.meshMaxHops } returns kotlinx.coroutines.flow.flowOf(5)
        every { settingsRepository.meshTtl } returns kotlinx.coroutines.flow.flowOf(10)
        
        meshRouter = MeshRouter(
            bleTransport = bleTransport,
            relayDao = relayDao,
            trustManager = mockk(relaxed = true),
            routingEngine = routingEngine,
            settingsRepository = settingsRepository,
            applicationScope = testScope.backgroundScope
        )
    }
    
    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `test deduplication of startStoreAndForwardLoop`() = testScope.runTest {
        // Mock the flow to prevent NotImplementedError if it's used
        
        // Start the loop
        meshRouter.startStoreAndForwardLoop()
        
        val firstJob = getPrivateProperty(meshRouter, "storeForwardJob") as Job?
        assertNotNull("storeForwardJob should be created", firstJob)
        assertTrue("storeForwardJob should be active", firstJob!!.isActive)
        
        // Try starting it again
        meshRouter.startStoreAndForwardLoop()
        val secondJob = getPrivateProperty(meshRouter, "storeForwardJob") as Job?
        
        assertEquals("Subsequent calls should not overwrite the active job", firstJob, secondJob)
    }

    @Test
    fun `test deduplication of observeIncoming`() = testScope.runTest {
        meshRouter.observeIncoming()
        
        val firstJob = getPrivateProperty(meshRouter, "incomingJob") as Job?
        assertNotNull("incomingJob should be created", firstJob)
        assertTrue("incomingJob should be active", firstJob!!.isActive)
        
        meshRouter.observeIncoming()
        val secondJob = getPrivateProperty(meshRouter, "incomingJob") as Job?
        
        assertEquals("Subsequent calls should not overwrite the active incomingJob", firstJob, secondJob)
    }

    @Test
    fun `test deduplication of startQueueProcessorLoop`() = testScope.runTest {
        meshRouter.startQueueProcessorLoop()
        
        val firstJob = getPrivateProperty(meshRouter, "queueProcessorJob") as Job?
        assertNotNull("queueProcessorJob should be created", firstJob)
        assertTrue("queueProcessorJob should be active", firstJob!!.isActive)
        
        meshRouter.startQueueProcessorLoop()
        val secondJob = getPrivateProperty(meshRouter, "queueProcessorJob") as Job?
        
        assertEquals("Subsequent calls should not overwrite the active queueProcessorJob", firstJob, secondJob)
    }

    private fun getPrivateProperty(instance: Any, propertyName: String): Any? {
        val field: Field = instance.javaClass.getDeclaredField(propertyName)
        field.isAccessible = true
        return field.get(instance)
    }
}
