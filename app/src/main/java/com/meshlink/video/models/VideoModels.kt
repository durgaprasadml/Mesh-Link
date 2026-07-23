package com.meshlink.video.models

import java.util.UUID

enum class CallState {
    IDLE,
    RINGING,
    CONNECTING,
    ACTIVE,
    HOLD,
    RECONNECTING,
    ENDING,
    ENDED,
    FAILED
}

enum class VideoCodecType {
    H265,
    H264,
    VP9
}

enum class Resolution(val width: Int, val height: Int) {
    R_240P(320, 240),
    R_360P(640, 360),
    R_480P(854, 480),
    R_720P(1280, 720),
    R_1080P(1920, 1080)
}

enum class CameraState {
    FRONT,
    REAR,
    OFF
}

enum class TransportType {
    BLE,
    WIFI_DIRECT,
    HYBRID
}

data class VideoSession(
    val callId: String = UUID.randomUUID().toString(),
    val initiatorId: String,
    val targetId: String,
    val isGroupCall: Boolean = false,
    var state: CallState = CallState.IDLE,
    var codec: VideoCodecType = VideoCodecType.H265,
    var resolution: Resolution = Resolution.R_720P,
    var fps: Int = 30,
    var transport: TransportType = TransportType.WIFI_DIRECT,
    var startTimeMs: Long = 0L,
    var participants: MutableSet<String> = mutableSetOf(initiatorId, targetId),
    var cameraState: CameraState = CameraState.FRONT,
    var isScreenSharing: Boolean = false
)

data class VideoFrame(
    val callId: String,
    val sequenceNumber: Long,
    val ptsUs: Long, // Presentation Time Stamp in microseconds
    val payload: ByteArray
)

data class VideoSignalingMessage(
    val type: SignalType,
    val callId: String,
    val targetId: String, // Or group ID
    val resolution: String = "R_720P",
    val codec: String = "H265",
    val isScreenShare: Boolean = false
)

enum class SignalType {
    INVITE,
    ACCEPT,
    REJECT,
    END,
    REQUEST_I_FRAME, // Tell sender to generate a keyframe due to packet loss
    PAUSE_VIDEO,
    RESUME_VIDEO
}
