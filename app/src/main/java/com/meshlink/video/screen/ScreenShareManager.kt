package com.meshlink.video.screen

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.Surface
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ScreenShareManager"
    }

    private val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    /**
     * Start capturing the screen. 
     * @param resultCode from the Activity onActivityResult where user granted permission
     * @param data Intent from the Activity onActivityResult
     * @param surface The MediaCodec input surface to draw the screen onto
     */
    fun startScreenCapture(resultCode: Int, data: Intent, surface: Surface, metrics: DisplayMetrics) {
        try {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // We use the encoder's input surface directly, achieving zero-copy screen recording.
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "MeshLinkScreenShare",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
            MeshLogger.d(TAG, "Screen capture started")
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Failed to start screen capture: ${e.message}")
        }
    }

    fun stopScreenCapture() {
        try {
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error stopping screen capture: ${e.message}")
        } finally {
            virtualDisplay = null
            mediaProjection = null
        }
    }
}
