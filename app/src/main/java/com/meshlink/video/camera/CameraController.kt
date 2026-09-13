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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume

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

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    suspend fun captureDualSosImages(
        sosEventId: String,
        lifecycleOwner: LifecycleOwner? = null
    ): SosDualCaptureResult = withContext(Dispatchers.IO) {
        if (!hasCameraPermission()) {
            return@withContext SosDualCaptureResult(
                frontError = "Camera permission not granted",
                rearError = "Camera permission not granted"
            )
        }

        initialize()
        val provider = cameraProvider
        if (provider == null) {
            return@withContext SosDualCaptureResult(
                frontError = "CameraProvider unavailable",
                rearError = "CameraProvider unavailable"
            )
        }

        val resolvedLifecycleOwner = lifecycleOwner ?: try {
            androidx.lifecycle.ProcessLifecycleOwner.get()
        } catch (e: Exception) {
            MeshLogger.w(TAG, "ProcessLifecycleOwner unavailable: ${e.message}")
            return@withContext SosDualCaptureResult(
                frontError = "LifecycleOwner unavailable: ${e.message}",
                rearError = "LifecycleOwner unavailable: ${e.message}"
            )
        }

        val outputDir = File(context.cacheDir, "sos_media").apply { mkdirs() }
        val frontFile = File(outputDir, "sos_${sosEventId}_front.jpg")
        val rearFile = File(outputDir, "sos_${sosEventId}_rear.jpg")

        var frontResultFile: File? = null
        var frontError: String? = null
        var rearResultFile: File? = null
        var rearError: String? = null

        try {
            // Check front camera
            val hasFront = try {
                provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            } catch (e: Exception) {
                false
            }

            if (hasFront) {
                MeshLogger.d(TAG, "Capturing FRONT camera for SOS $sosEventId")
                val res = captureImageWithSelector(
                    provider,
                    resolvedLifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    frontFile
                )
                if (res.isSuccess) {
                    frontResultFile = res.getOrNull()
                } else {
                    frontError = res.exceptionOrNull()?.message ?: "Front camera capture failed"
                }
            } else {
                frontError = "Front camera hardware not available"
            }

            // Check rear camera
            val hasRear = try {
                provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            } catch (e: Exception) {
                false
            }

            if (hasRear) {
                MeshLogger.d(TAG, "Capturing REAR camera for SOS $sosEventId")
                val res = captureImageWithSelector(
                    provider,
                    resolvedLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    rearFile
                )
                if (res.isSuccess) {
                    rearResultFile = res.getOrNull()
                } else {
                    rearError = res.exceptionOrNull()?.message ?: "Rear camera capture failed"
                }
            } else {
                rearError = "Rear camera hardware not available"
            }
        } finally {
            // Camera resource cleanup: unbind all use cases immediately
            try {
                withContext(Dispatchers.Main) {
                    provider.unbindAll()
                }
                MeshLogger.d(TAG, "Camera resources unbound and cleaned up for SOS $sosEventId")
            } catch (e: Exception) {
                MeshLogger.w(TAG, "Error unbinding camera: ${e.message}")
            }
        }

        SosDualCaptureResult(
            frontImage = frontResultFile,
            rearImage = rearResultFile,
            frontError = frontError,
            rearError = rearError
        )
    }

    private suspend fun captureImageWithSelector(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                val imageCapture = androidx.camera.core.ImageCapture.Builder()
                    .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Binding to lifecycle must happen on main thread in CameraX
                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, imageCapture)

                        val outputOptions = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(outputFile).build()

                        imageCapture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: androidx.camera.core.ImageCapture.OutputFileResults) {
                                    MeshLogger.d(TAG, "Image saved successfully: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                                    if (continuation.isActive) {
                                        continuation.resume(Result.success(outputFile))
                                    }
                                }

                                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                    MeshLogger.e(TAG, "Image capture error: ${exception.message}", exception)
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(exception))
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        MeshLogger.e(TAG, "Failed to bind camera on main thread: ${e.message}", e)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(e))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                        try {
                            provider.unbindAll()
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                MeshLogger.e(TAG, "Failed in captureImageWithSelector: ${e.message}", e)
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }
}

data class SosDualCaptureResult(
    val frontImage: File? = null,
    val rearImage: File? = null,
    val frontError: String? = null,
    val rearError: String? = null
) {
    val hasAtLeastOneImage: Boolean get() = frontImage != null || rearImage != null
    val bothSucceeded: Boolean get() = frontImage != null && rearImage != null
}

