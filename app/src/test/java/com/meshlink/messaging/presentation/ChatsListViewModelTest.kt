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

    @Test
    fun `uiState preserves unread counts and search query across process recreation`() = runTest {
        val chats = listOf(
            Chat(id = "c1", name = "Alice", lastMessage = "Hi", lastMessageAt = 100L, unreadCount = 2),
            Chat(id = "c2", name = "Bob", lastMessage = "Hey", lastMessageAt = 200L, unreadCount = 3)
        )
        every { getAllChatsUseCase() } returns flowOf(chats)

        val savedState = androidx.lifecycle.SavedStateHandle(
            mapOf(ChatsListViewModel.KEY_SEARCH_QUERY to "Alice")
        )
        val recreatedVm = ChatsListViewModel(
            getAllChatsUseCase = getAllChatsUseCase,
            defaultDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
            savedStateHandle = savedState
        )

        recreatedVm.uiState.test {
            var state = awaitItem()
            if (state.chats.isEmpty()) {
                state = awaitItem()
            }
            assertEquals("Alice", state.searchQuery)
            assertEquals(1, state.chats.size)
            assertEquals(2, state.chats[0].unreadCount)
            assertEquals(5, state.totalUnreadCount)
        }
    }
}
