package com.meshlink.ui.sos

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.meshlink.data.location.LocationProvider
import com.meshlink.domain.repository.MeshRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SosViewModelTest {

    private val meshRepository: MeshRepository = mockk(relaxed = true)
    private val locationProvider: LocationProvider = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val cameraManager: CameraManager = mockk(relaxed = true)

    private lateinit classUnderTest: SosViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        classUnderTest = SosViewModel(meshRepository, locationProvider, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleFlashlight turns flashlight on when currently off`() {
        every { cameraManager.cameraIdList } returns arrayOf("0")
        
        classUnderTest.toggleFlashlight()
        
        assertEquals(true, classUnderTest.uiState.value.isFlashlightOn)
        verify { cameraManager.setTorchMode("0", true) }
    }

    @Test
    fun `toggleFlashlight turns flashlight off when currently on`() {
        every { cameraManager.cameraIdList } returns arrayOf("0")
        
        // Turn on
        classUnderTest.toggleFlashlight()
        
        // Turn off
        classUnderTest.toggleFlashlight()
        
        assertEquals(false, classUnderTest.uiState.value.isFlashlightOn)
        verify { cameraManager.setTorchMode("0", false) }
    }

    @Test
    fun `toggleFlashlight sets error message if camera unavailable`() {
        every { cameraManager.cameraIdList } throws RuntimeException("Camera error")
        
        classUnderTest.toggleFlashlight()
        
        assertEquals(false, classUnderTest.uiState.value.isFlashlightOn)
        assertEquals("Flashlight unavailable: Camera error", classUnderTest.uiState.value.errorMessage)
    }
}
