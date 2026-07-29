package com.meshlink.ui.landing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Pre-allocated star node representing a single peer in the Mesh Network universe — v4.
 *
 * New fields (v4):
 *  • [willMigrate]         — decided at generation time (40% of text nodes); only these migrate
 *                            to letter positions in Scene 7. The other 60% remain as universe stars.
 *  • [signatureWaveFlash]  — 0→1 flash intensity set when the heartbeat wave front reaches
 *                            this specific node (enables per-node staggered acknowledgement).
 *  • [isPacketReversal]    — marks this node as the convergence target during the Mesh Link
 *                            signature moment (all packets reverse toward it).
 */
class AnimatedMeshNode(
    val id: Int,
    val startXRatio: Float,
    val startYRatio: Float,
    var targetXRatio: Float = startXRatio,
    var targetYRatio: Float = startYRatio,
    var radiusDp: Float = 2.5f,
    val glowColor: Color = AnimationConstants.StarlightSilver,
    val pulsePhase: Float = 0f,
    val pulseSpeed: Float = 1.0f,
    val twinklePhase: Float = 0f,
    val twinkleSpeed: Float = 1.0f,
    val baseBrightness: Float = 0.4f,
    val isMigrating: Boolean = false,
    val migrationOrder: Float = 0f,
    val isUserNode: Boolean = false,
    val depthLayer: DepthLayer = DepthLayer.MIDGROUND,
    val isRelayHub: Boolean = false,
    val relayScore: Float = 1.0f,
    val breathingPhase: Float = 0f,
    val wakeDelay: Float = 0f,
    val isSilenceSentinel: Boolean = false,

    // ── v4: Migration control ─────────────────────────────────────────────────
    /** True if this node should migrate to a letter position (40% selection via launch seed).
     *  False = this node stays as a universe star even after title formation begins. */
    val willMigrate: Boolean = false,

    // ── v4: Signature heartbeat ───────────────────────────────────────────────
    /** True if this node is the designated convergence target for the packet reversal signature. */
    val isPacketReversalTarget: Boolean = false
) {
    // Computed pixel coordinates (parallax-adjusted)
    var currentX by mutableFloatStateOf(0f)
    var currentY by mutableFloatStateOf(0f)

    // Un-parallaxed raw physics position
    var rawX by mutableFloatStateOf(0f)
    var rawY by mutableFloatStateOf(0f)

    // ── Dynamic animation state ──────────────────────────────────────────────
    var alpha               by mutableFloatStateOf(0f)
    var currentBrightness   by mutableFloatStateOf(baseBrightness)
    var isDiscovered        by mutableStateOf(false)
    var discoveryProgress   by mutableFloatStateOf(0f)
    var pulseIntensity      by mutableFloatStateOf(0f)
    var dofBlurFactor       by mutableFloatStateOf(0f)

    /** 0→1 ramp: 0 = invisible (not yet awakened), 1 = fully visible. */
    var wakeProgress by mutableFloatStateOf(
        if (isSilenceSentinel) 1f else 0f
    )

    /** v4: 0→1 flash intensity when the heartbeat wave front passes through this node.
     *  Decays each frame at ~3.5/s (managed in NodePhysics.updatePositions). */
    var signatureWaveFlash by mutableFloatStateOf(0f)

    fun reset() {
        alpha              = 0f
        currentBrightness  = baseBrightness
        isDiscovered       = false
        discoveryProgress  = 0f
        pulseIntensity     = 0f
        dofBlurFactor      = 0f
        wakeProgress       = if (isSilenceSentinel) 1f else 0f
        signatureWaveFlash = 0f
    }
}
