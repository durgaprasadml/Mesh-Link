package com.meshlink.domain.usecase.auth

import android.content.Context
import androidx.work.WorkManager
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.SessionManager
import com.meshlink.wifi.data.WifiDirectManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogoutUseCaseTest {

    private lateinit var context: Context
    private lateinit var userRepository: UserRepository
    private lateinit var meshRepository: MeshRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var wifiDirectManager: WifiDirectManager
    private lateinit var workManager: WorkManager
    private lateinit var logoutUseCase: LogoutUseCase

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        meshRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        wifiDirectManager = mockk(relaxed = true)
        workManager = mockk(relaxed = true)

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        logoutUseCase = LogoutUseCase(
            context = context,
            userRepository = userRepository,
            meshRepository = meshRepository,
            sessionManager = sessionManager,
            wifiDirectManager = wifiDirectManager
        )
    }

    @Test
    fun `invoke should cancel work manager, stop mesh relay service, disconnect wifi, terminate sessions, and clear user session`() = runTest {
        // Arrange
        val clearData = true

        // Act
        logoutUseCase(clearData)

        // Assert
        verify { workManager.cancelAllWork() }
        verify { context.startService(any()) }
        verify { meshRepository.stopMesh() }
        verify { wifiDirectManager.disconnect() }
        verify { sessionManager.terminateAllSessions() }
        coVerify { userRepository.logout(clearData) }
    }
}
