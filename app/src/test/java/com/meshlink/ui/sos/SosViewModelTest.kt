package com.meshlink.ui.sos

import android.content.Context
import android.hardware.camera2.CameraManager
import com.meshlink.data.location.LocationProvider
import com.meshlink.domain.model.MeshResult
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.video.camera.CameraController
import com.meshlink.video.camera.SosDualCaptureResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

import com.meshlink.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val meshRepository: MeshRepository = mockk(relaxed = true)
    private val locationProvider: LocationProvider = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val cameraManager: CameraManager = mockk(relaxed = true)
    private val cameraController: CameraController = mockk(relaxed = true)

    private lateinit var classUnderTest: SosViewModel

    @Before
    fun setup() {
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        every { meshRepository.scannedDevices } returns MutableStateFlow(emptyMap())
        classUnderTest = SosViewModel(meshRepository, locationProvider, context, cameraController)
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

    @Test
    fun `sendSos with camera permission triggers immediate dual capture and sends SOS media`() = runTest(testDispatcher) {
        val dummyFront = File.createTempFile("test_front", ".jpg")
        val dummyRear = File.createTempFile("test_rear", ".jpg")
        every { cameraController.hasCameraPermission() } returns true
        coEvery { cameraController.captureDualSosImages(any(), any()) } returns SosDualCaptureResult(
            frontImage = dummyFront,
            rearImage = dummyRear
        )
        coEvery { meshRepository.dispatchSos(any()) } returns MeshResult.Success(Unit)

        classUnderTest.sendSos()
        advanceUntilIdle()

        val state = classUnderTest.uiState.value
        assertEquals(SosStatus.DELIVERED, state.status)
        assertTrue(state.sosSent)
        assertEquals(dummyFront.absolutePath, state.frontImagePath)
        assertEquals(dummyRear.absolutePath, state.rearImagePath)
        assertEquals("Front and rear cameras captured", state.cameraCaptureStatus)

        coVerify { meshRepository.dispatchSos(any()) }
        coVerify { cameraController.captureDualSosImages(any(), any()) }
        coVerify { meshRepository.sendSosMedia(any(), dummyFront, dummyRear) }

        dummyFront.delete()
        dummyRear.delete()
    }

    @Test
    fun `sendSos handles camera permission denied gracefully without crashing`() = runTest(testDispatcher) {
        every { cameraController.hasCameraPermission() } returns false
        coEvery { meshRepository.dispatchSos(any()) } returns MeshResult.Success(Unit)

        classUnderTest.sendSos()
        advanceUntilIdle()

        val state = classUnderTest.uiState.value
        assertEquals(SosStatus.DELIVERED, state.status)
        assertTrue(state.sosSent)
        assertEquals(null, state.frontImagePath)
        assertEquals(null, state.rearImagePath)
        assertEquals("Camera permission denied; alert broadcasted without photos.", state.cameraCaptureStatus)

        coVerify { meshRepository.dispatchSos(any()) }
        coVerify(exactly = 0) { cameraController.captureDualSosImages(any(), any()) }
        coVerify(exactly = 0) { meshRepository.sendSosMedia(any(), any(), any()) }
    }

    @Test
    fun `sendSos handles partial camera capture when rear camera fails`() = runTest(testDispatcher) {
        val dummyFront = File.createTempFile("test_front", ".jpg")
        every { cameraController.hasCameraPermission() } returns true
        coEvery { cameraController.captureDualSosImages(any(), any()) } returns SosDualCaptureResult(
            frontImage = dummyFront,
            rearImage = null,
            rearError = "Rear camera hardware in use"
        )
        coEvery { meshRepository.dispatchSos(any()) } returns MeshResult.Success(Unit)

        classUnderTest.sendSos()
        advanceUntilIdle()

        val state = classUnderTest.uiState.value
        assertEquals(SosStatus.DELIVERED, state.status)
        assertEquals(dummyFront.absolutePath, state.frontImagePath)
        assertEquals(null, state.rearImagePath)
        assertTrue(state.cameraCaptureStatus?.contains("rear failed") == true)

        coVerify { meshRepository.sendSosMedia(any(), dummyFront, null) }

        dummyFront.delete()
    }

    @Test
    fun `sendSos prevents duplicate trigger when already sending`() = runTest(testDispatcher) {
        every { cameraController.hasCameraPermission() } returns true
        coEvery { cameraController.captureDualSosImages(any(), any()) } returns SosDualCaptureResult(
            frontImage = null,
            rearImage = null
        )
        coEvery { meshRepository.dispatchSos(any()) } returns MeshResult.Success(Unit)

        // First call
        classUnderTest.sendSos()
        // Second call while first is in-flight
        classUnderTest.sendSos()
        advanceUntilIdle()

        coVerify(exactly = 1) { meshRepository.dispatchSos(any()) }
    }
}

