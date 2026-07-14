package com.meshlink.voice.models

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

enum class CodecType {
    AAC,
    OPUS,
    PCM // For uncompressed testing/fallback
}

enum class TransportType {
    BLE,
    WIFI_DIRECT,
    HYBRID
}

data class VoiceSession(
    val callId: String = UUID.randomUUID().toString(),
    val initiatorId: String,
    val targetId: String,
    val isGroupCall: Boolean = false,
    val isPttMode: Boolean = false,
    var state: CallState = CallState.IDLE,
    var codec: CodecType = CodecType.AAC,
    var transport: TransportType = TransportType.BLE,
    var startTimeMs: Long = 0L,
    var participants: MutableSet<String> = mutableSetOf(initiatorId, targetId)
)

data class AudioFrame(
    val callId: String,
    val sequenceNumber: Long,
    val payload: ByteArray,
    val isSilent: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class SignalingMessage(
    val type: SignalType,
    val callId: String,
    val targetId: String, // Or group ID
    val codec: String = "AAC",
    val isPtt: Boolean = false
)

enum class SignalType {
    INVITE,
    ACCEPT,
    REJECT,
    END,
    PTT_START,
    PTT_STOP
}
