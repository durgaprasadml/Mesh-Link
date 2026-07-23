package com.meshlink.video.camera

import android.content.Context
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.guava.await

@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraController"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var currentLensFacing = CameraSelector.LENS_FACING_FRONT
    
    private var preview: Preview? = null
    private var encoderSurfaceProvider: Surface? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    suspend fun initialize() {
        if (cameraProvider == null) {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(context).await()
                MeshLogger.d(TAG, "CameraProvider initialized")
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed to get CameraProvider: ${e.message}")
            }
        }
    }

    /**
     * Binds the camera to a lifecycle (e.g. Activity or Fragment) and provides a Surface 
     * from the VideoCodecManager (encoderSurface).
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner, 
        encoderSurface: Surface, 
        resolution: Size = Size(1280, 720)
    ) {
        val provider = cameraProvider ?: return
        this.encoderSurfaceProvider = encoderSurface

        try {
            provider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(currentLensFacing)
                .build()

            // We use Preview instead of ImageAnalysis because we want to pipe the 
            // camera stream directly into a MediaCodec Surface for zero-copy hardware encoding.
            preview = Preview.Builder()
                .setTargetResolution(resolution)
                .build()

            // When CameraX requests a surface, we give it the MediaCodec encoder surface
            preview?.setSurfaceProvider { request: SurfaceRequest ->
                request.provideSurface(encoderSurface, cameraExecutor) { result ->
                    MeshLogger.d(TAG, "Surface request result: ${result.resultCode}")
                }
            }

            camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            MeshLogger.d(TAG, "Camera started")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start camera: ${e.message}")
        }
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            camera = null
            preview = null
            MeshLogger.d(TAG, "Camera stopped")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to stop camera: ${e.message}")
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, resolution: Size = Size(1280, 720)) {
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        
        encoderSurfaceProvider?.let { surface ->
            startCamera(lifecycleOwner, surface, resolution)
        }
    }

    fun toggleTorch(enable: Boolean) {
        camera?.cameraControl?.enableTorch(enable)
    }
}
