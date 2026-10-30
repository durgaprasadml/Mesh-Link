package com.meshlink.service

import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.meshlink.common.logger.MeshLogger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class BootCompletedReceiverTest {

    private lateinit var context: Context
    private lateinit var userManager: UserManager
    private lateinit var lifecycleManager: MeshLifecycleManager
    private lateinit var receiver: BootCompletedReceiver

    @Before
    fun setUp() {
        mockkObject(MeshLogger)
        every { MeshLogger.d(any<String>(), any<String>()) } just runs
        every { MeshLogger.i(any<String>(), any<String>()) } just runs
        every { MeshLogger.w(any<String>(), any<String>()) } just runs
        every { MeshLogger.e(any<String>(), any<String>(), any()) } just runs

        context = mockk(relaxed = true)
        userManager = mockk(relaxed = true)
        lifecycleManager = mockk(relaxed = true)
        every { context.getSystemService(UserManager::class.java) } returns userManager

        receiver = BootCompletedReceiver().apply {
            meshLifecycleManager = lifecycleManager
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `handleBootIntent skips initialization when device is in Direct Boot locked state`() {
        every { userManager.isUserUnlocked } returns false

        receiver.handleBootIntent(context, Intent.ACTION_BOOT_COMPLETED)
        receiver.handleBootIntent(context, "android.intent.action.LOCKED_BOOT_COMPLETED")

        verify(exactly = 0) { lifecycleManager.initialize() }
        verify(exactly = 0) { lifecycleManager.startMesh() }
        verify(exactly = 0) { context.startService(any()) }
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `handleBootIntent ignores TIME_SET and TIMEZONE_CHANGED broadcasts`() {
        every { userManager.isUserUnlocked } returns true

        receiver.handleBootIntent(context, Intent.ACTION_TIME_CHANGED)
        receiver.handleBootIntent(context, Intent.ACTION_TIMEZONE_CHANGED)

        verify(exactly = 0) { lifecycleManager.initialize() }
        verify(exactly = 0) { lifecycleManager.startMesh() }
        verify(exactly = 0) { context.startService(any()) }
        verify(exactly = 0) { context.startForegroundService(any()) }
    }

    @Test
    fun `handleBootIntent initializes and starts mesh when unlocked and BOOT_COMPLETED received`() {
        every { userManager.isUserUnlocked } returns true

        receiver.handleBootIntent(context, Intent.ACTION_BOOT_COMPLETED)

        verify(exactly = 1) { lifecycleManager.initialize() }
        verify(exactly = 1) { lifecycleManager.startMesh() }
    }
}
