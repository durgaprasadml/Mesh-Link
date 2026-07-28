package com.meshlink.ui.analytics

import com.meshlink.analytics.data.LogType
import com.meshlink.analytics.data.MeshAnalytics
import com.meshlink.analytics.data.MeshStats
import com.meshlink.analytics.data.RelayLogEntry
import com.meshlink.domain.repository.MeshRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analytics = mockk<MeshAnalytics>(relaxed = true)
    private val meshRepository = mockk<MeshRepository>(relaxed = true)

    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { analytics.stats } returns MutableStateFlow(MeshStats(packetsRelayed = 42))
        every { analytics.recentRelayLog } returns MutableStateFlow(listOf(RelayLogEntry(System.currentTimeMillis(), "RELAY", "pkt_1 to node_b", LogType.RELAY)))
        every { analytics.activeNodes } returns MutableStateFlow(setOf("node_a", "node_b"))
        every { analytics.hopDistribution } returns MutableStateFlow(mapOf(1 to 10, 2 to 5))
        every { meshRepository.getRouteTable() } returns mapOf("dest_1" to "next_hop_1")

        viewModel = AnalyticsViewModel(analytics, meshRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `uiState combines analytics flows and routeTable`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(42, state.stats.packetsRelayed)
        assertEquals(1, state.recentLog.size)
        assertEquals("RELAY", state.recentLog.first().event)
        assertEquals(2, state.activeNodes.size)
        assertEquals(1, state.routeTableSize)
        assertEquals("next_hop_1", state.routeTable["dest_1"])
    }
}
