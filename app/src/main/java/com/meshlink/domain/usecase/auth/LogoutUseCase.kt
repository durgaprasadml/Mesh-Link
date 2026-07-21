package com.meshlink.domain.usecase.auth

import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.SessionManager
import com.meshlink.service.MeshRelayService
import com.meshlink.wifi.data.WifiDirectManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val meshRepository: MeshRepository,
    private val sessionManager: SessionManager,
    private val wifiDirectManager: WifiDirectManager
) {
    suspend operator fun invoke(clearLocalData: Boolean = false) {
        // 1. Cancel background workers
        WorkManager.getInstance(context).cancelAllWork()

        // 2. Stop Mesh Relay Service
        val intent = Intent(context, MeshRelayService::class.java).apply {
            action = MeshRelayService.ACTION_STOP
        }
        context.startService(intent)

        // 3. Stop Mesh (Disconnects BLE and other mesh-related components)
        meshRepository.stopMesh()

        // 4. Disconnect Wi-Fi Direct
        wifiDirectManager.disconnect()

        // 5. Terminate all active secure sessions
        sessionManager.terminateAllSessions()

        // 6. Clear user authentication session and optionally local data
        userRepository.logout(clearData = clearLocalData)
    }
}
