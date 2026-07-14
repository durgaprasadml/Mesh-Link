package com.meshlink.ui.home

import app.cash.turbine.test
import com.meshlink.database.data.local.ChatDao
import com.meshlink.database.data.local.ChatEntity
import com.meshlink.database.data.local.UserEntity
import com.meshlink.domain.model.BleDevice
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userRepository: UserRepository
    private lateinit var meshRepository: MeshRepository
    private lateinit var chatDao: ChatDao
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        userRepository = mockk(relaxed = true)
        meshRepository = mockk(relaxed = true)
        chatDao = mockk(relaxed = true)
    }

    @Test
    fun `loadUser sets user state correctly`() = runTest {
        val userEntity = UserEntity("mesh_1", "John", "123", "hash")
        coEvery { userRepository.getLocalUser() } returns userEntity
        every { meshRepository.scannedDevices } returns MutableStateFlow(emptyMap())
        every { chatDao.getAllChats() } returns flowOf(emptyList())

        viewModel = HomeViewModel(userRepository, meshRepository, chatDao)

        viewModel.user.test {
            val user = awaitItem()
            if (user == null) {
                assertEquals(userEntity, awaitItem())
            } else {
                assertEquals(userEntity, user)
            }
        }
    }

    @Test
    fun `uiState combines data correctly`() = runTest {
        val userEntity = UserEntity("mesh_1", "John", "123", "hash")
        val device = BleDevice("mesh_id", "TestDevice", "00:11:22", -50, 1000L)
        val chatEntity = ChatEntity("chat_1", "Chat 1", "msg_1", 1000L, unreadCount = 2)
        
        coEvery { userRepository.getLocalUser() } returns userEntity
        val devicesFlow = MutableStateFlow(mapOf("00:11:22" to device))
        every { meshRepository.scannedDevices } returns devicesFlow
        every { chatDao.getAllChats() } returns flowOf(listOf(chatEntity))

        viewModel = HomeViewModel(userRepository, meshRepository, chatDao)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(userEntity, state.user)
            assertEquals(1, state.nearbyDevices.size)
            assertEquals(device, state.nearbyDevices[0])
            assertEquals(2, state.unreadChatsCount)
        }
    }
}
