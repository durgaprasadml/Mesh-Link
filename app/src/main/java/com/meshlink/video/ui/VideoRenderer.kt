package com.meshlink.video.ui

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.meshlink.video.codec.VideoCodecManager
import com.meshlink.video.streaming.VideoStreamManager

@Composable
fun RemoteVideoRenderer(
    codecManager: VideoCodecManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // We use a SurfaceView for hardware-accelerated zero-copy rendering from MediaCodec
    val surfaceView = remember { SurfaceView(context) }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                surfaceView.apply {
                    holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            // Tell the decoder to start writing frames to this surface
                            // Resolution should be matched to session state, hardcoded to 720p for now
                            codecManager.startDecoder(holder.surface, 720, 1280)
                        }

                        override fun surfaceChanged(
                            holder: android.view.SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {}

                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                            codecManager.stopDecoder()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LocalVideoRenderer(
    streamManager: VideoStreamManager,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    DisposableEffect(lifecycleOwner) {
        streamManager.bindLifecycle(lifecycleOwner)
        onDispose {
            // Unbind or pause camera if necessary
        }
    }
    
    // For the local preview, since CameraX is bound to the encoder's input surface directly,
    // we don't render it separately to a view here unless we want to use CameraX PreviewView.
    // However, CameraX only allows one Surface provider per Preview use-case.
    // To show local preview AND encode, we actually need to attach an ImageAnalysis use-case
    // or use OpenGL to split the Surface texture.
    // For simplicity in Phase E6, we will assume the caller uses a PreviewView if they bind it separately,
    // but the actual stream goes to the encoder.
    // Full implementation of a split-surface is required for a polished UI.
}
