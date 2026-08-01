package com.meshlink.ui.designsystem.theme.haptics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.meshlink.ui.designsystem.theme.colors.MeshColorTokens

/**
 * Interaction System & State Specification for Mesh-Link 2026 Original Design System.
 */
@Immutable
enum class MeshInteractionState {
    IDLE,
    PRESSED,
    FOCUSED,
    HOVERED,
    SELECTED,
    ACTIVE,
    INACTIVE,
    DISABLED,
    LOADING,
    SUCCESS,
    FAILURE,
    SEARCHING,
    CONNECTED,
    DISCONNECTED,
    EMERGENCY,
    BROADCASTING,
    RECEIVING,
    SENDING,
    ENCRYPTED,
    OFFLINE
}

@Immutable
object MeshInteractionRules {
    const val PRESSED_SCALE = 0.97f
    const val PRESSED_ALPHA = 0.85f
    const val DISABLED_ALPHA = 0.38f
    const val HOVER_ALPHA = 0.08f
    const val FOCUS_STROKE_WIDTH_DP = 2.0f

    fun stateColor(state: MeshInteractionState): Color {
        return when (state) {
            MeshInteractionState.CONNECTED -> MeshColorTokens.MeshConnected
            MeshInteractionState.DISCONNECTED, MeshInteractionState.OFFLINE -> MeshColorTokens.MeshDisconnected
            MeshInteractionState.SEARCHING -> MeshColorTokens.MeshSearching
            MeshInteractionState.EMERGENCY -> MeshColorTokens.EmergencyCrimson
            MeshInteractionState.BROADCASTING -> MeshColorTokens.MeshBroadcasting
            MeshInteractionState.ENCRYPTED -> MeshColorTokens.MeshEncrypted
            MeshInteractionState.SUCCESS -> MeshColorTokens.SuccessGreen
            MeshInteractionState.FAILURE -> MeshColorTokens.DangerRed
            MeshInteractionState.SENDING, MeshInteractionState.RECEIVING -> MeshColorTokens.QuantumCyan
            else -> MeshColorTokens.CyberMint
        }
    }
}
