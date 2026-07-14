package com.meshlink.messaging.presentation

import app.cash.turbine.test
import com.meshlink.domain.model.Chat
import com.meshlink.domain.usecase.messaging.GetAllChatsUseCase
import com.meshlink.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getAllChatsUseCase: GetAllChatsUseCase
    private lateinit var viewModel: ChatsListViewModel

    @Before
    fun setup() {
        getAllChatsUseCase = mockk(relaxed = true)
    }

    @Test
    fun `uiState emits list of chats from use case`() = runTest {
        val chats = listOf(mockk<Chat>(), mockk<Chat>())
        every { getAllChatsUseCase() } returns flowOf(chats)

        viewModel = ChatsListViewModel(getAllChatsUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            if (state.chats.isEmpty()) {
                val nextState = awaitItem()
                assertEquals(chats, nextState.chats)
            } else {
                assertEquals(chats, state.chats)
            }
        }
    }
}
