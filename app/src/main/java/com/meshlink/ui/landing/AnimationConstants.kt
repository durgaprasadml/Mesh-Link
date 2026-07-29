package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color

/**
 * Design system tokens for the Mesh Link Cinematic Landing Experience
 * (v4 — Flagship Refinement Edition).
 *
 * Duration compressed: 13 s → 5.5 s (existing) / 7.5 s (first-time).
 * All 10 scenes are preserved; they are simply tighter.
 *
 * Compressed scene map (normalized 0→1):
 *   Scene 1  Silence              0.00 → 0.06   ~0.33 s
 *   Scene 2  First Discovery      0.06 → 0.16   ~0.55 s
 *   Scene 3  Organic Expansion    0.16 → 0.42   ~1.43 s
 *   Scene 4  Living Network       0.42 → 0.54   ~0.66 s
 *   Scene 5  Camera Journey       0.48 → 0.65   overlaps
 *   Scene 6  Hidden Identity      0.54 → 0.64   ~0.55 s
 *   Scene 7  Title Formation      0.64 → 0.80   ~0.88 s
 *   Scene 8  Living Logo          0.80 → 0.86   ~0.33 s
 *   Scene 9  Synchronization Wave 0.86 → 0.93   ~0.38 s
 *   Scene 10 Entering the Mesh   0.93 → 1.00   ~0.38 s
 */
object AnimationConstants {

    // ── Monochrome Starlight Color Palette ──────────────────────────────────
    val DeepSpaceNavy          = Color(0xFF02040A)
    val SpaceDarkCharcoal      = Color(0xFF050916)
    val StarlightWhite         = Color(0xFFF6F9FB)
    val StarlightWhiteGlow     = Color(0x50FFFFFF)
    val StarlightSilver        = Color(0xFFB0C4D6)
    val StarlightSilverGlow    = Color(0x2EB0C4D6)
    val StarGreyDim            = Color(0xFF364858)
    val StarGreyDimTransparent = Color(0x18364858)
    val SubtleStarlightBlue    = Color(0xFFCCE8FF)
    val SoftWhiteTransparent   = Color(0x78EBF2F8)
    val DimBackground          = Color(0xC802040A)

    // Ignition — barely perceptible warm dawn flash
    val IgnitionGold           = Color(0x14FFE5A0)

    // Data packets — cold blue-white, distinct from node stars
    val PacketCyan             = Color(0x99C8E8FF)
    val PacketCyanGlow         = Color(0x2898CCFF)

    // ── Atmospheric / Environmental Colors (v4) ──────────────────────────────
    // Volumetric light rays — extremely subtle, max alpha 0.04
    val VolumetricRayWhite     = Color(0x0AFFFFFF)
    // Secondary nebula haze layer — warm purple tint, barely visible
    val NebulaPurple           = Color(0x08A080D0)
    // Cosmic dust — cold blue-grey micro-particles
    val CosmicDustAmber        = Color(0x0660788C)

    // ── Connection Colors ────────────────────────────────────────────────────
    val ConnectionDiscovery     = Color(0x55B0C4D6)
    val ConnectionConnected     = Color(0xBBF6F9FB)
    val ConnectionConstellation = Color(0xFFF6F9FB)
    val ConnectionInactive      = Color(0x0A364858)
    val MultiHopRelayColor      = Color(0x22B0C4D6)

    // ── Timing (v4 — compressed) ──────────────────────────────────────────────
    const val STARTUP_ANIMATION_DURATION_MS = 5_500L
    const val WELCOME_ANIMATION_DURATION_MS = 7_500L

    // ── 10-Scene Phase Boundaries (normalized 0.0 → 1.0) ────────────────────
    const val SCENE_1_END   = 0.06f   // Silence
    const val SCENE_2_END   = 0.16f   // First Discovery
    const val SCENE_3_END   = 0.42f   // Organic Expansion
    const val SCENE_4_END   = 0.54f   // Living Network
    const val SCENE_5_START = 0.48f   // Camera Journey start (overlaps 4)
    const val SCENE_6_END   = 0.64f   // Hidden Identity
    const val SCENE_7_END   = 0.80f   // Title Formation
    const val SCENE_8_END   = 0.86f   // Living Logo
    const val SCENE_9_END   = 0.93f   // Synchronization Wave

    // Legacy aliases (kept for compatibility with existing callers)
    const val PHASE_1_TWINKLE_END                 = SCENE_1_END
    const val PHASE_2_SEED_IGNITION_END           = SCENE_2_END
    const val PHASE_3_WAVE_PROPAGATION_END        = SCENE_3_END
    const val PHASE_4_CONSTELLATION_MIGRATION_END = SCENE_7_END
    const val PHASE_5_LIVING_CONSTELLATION_END    = SCENE_8_END
    const val PHASE_6_FINAL_PULSE_ZOOM_END        = 1.00f

    // Signature moment — Scene 9 heartbeat wave (v4: graph-propagated)
    const val SIGNATURE_CONVERGENCE_START = SCENE_8_END
    const val SIGNATURE_FLASH_PEAK        = 0.895f
    const val SIGNATURE_FLASH_FADE        = SCENE_9_END

    // Packet reversal moment — just before Scene 9 (Mesh Link signature)
    const val PACKET_REVERSAL_START       = SCENE_8_END - 0.025f

    // ── Migration Control (v4) ───────────────────────────────────────────────
    /** Fraction of midground text-nodes that will actually migrate to letter positions.
     *  The remaining (1 - MIGRATION_RATIO) continue as natural universe stars. */
    const val MIGRATION_RATIO = 0.40f

    // ── Signature Wave Origin Jitter (v4) ────────────────────────────────────
    /** Base origin for the radial signature wave (bottom-left region). */
    const val SIGNATURE_ORIGIN_X_BASE = 0.12f
    const val SIGNATURE_ORIGIN_Y_BASE = 0.88f
    /** Max random jitter applied per launch (±0.06 on each axis). */
    const val SIGNATURE_ORIGIN_JITTER = 0.06f

    // ── Parallax ─────────────────────────────────────────────────────────────
    const val BG_PARALLAX_FACTOR  = 0.22f
    const val MID_PARALLAX_FACTOR = 1.0f
    const val FG_PARALLAX_FACTOR  = 1.8f

    // ── Node / Star Counts ───────────────────────────────────────────────────
    const val TOTAL_STAR_COUNT          = 120
    const val FG_STAR_COUNT             = 12
    const val MAX_CONNECTIONS_PER_NODE  = 3
    const val STAR_CONNECT_RADIUS_RATIO = 0.26f

    // ── Sizes ────────────────────────────────────────────────────────────────
    const val MIN_STAR_RADIUS_DP        = 0.9f
    const val MAX_STAR_RADIUS_DP        = 2.8f
    const val SEED_STAR_RADIUS_DP       = 5.5f
    const val USER_AVATAR_STAR_RADIUS_DP = 30.0f
    const val RELAY_HUB_RADIUS_FACTOR   = 1.5f

    // ── Breathing ────────────────────────────────────────────────────────────
    const val BREATHING_AMPLITUDE = 0.07f

    // ── Camera drift jitter ranges (v4) ──────────────────────────────────────
    /** Per-launch jitter added to each Lissajous frequency component. Range ±value. */
    const val CAMERA_FREQ_JITTER = 0.018f
}

/**
 * Visual classification of connection edges in the mesh network.
 */
enum class ConnectionType {
    DISCOVERY,
    CONNECTED,
    CONSTELLATION,
    MULTI_HOP_RELAY,
    INACTIVE
}

/**
 * Plasma growth stage of an edge connection:
 * spark → light streak → energy → stable glow.
 */
enum class PlasmaGrowthStage {
    SPARK,
    STREAK,
    FULL_CONNECTION,
    STABILIZING,
    STABLE_GLOW
}

/**
 * Depth layer classification for camera parallax and depth of field.
 */
enum class DepthLayer {
    BACKGROUND,
    MIDGROUND,
    FOREGROUND
}
