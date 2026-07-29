package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color

/**
 * Design system tokens, color palettes, timing constants, and configuration parameters
 * for the Mesh Link Animated Landing Experience.
 */
object AnimationConstants {

    // Palette Colors (Material 3 Dark Glassmorphism)
    val DeepNavy = Color(0xFF0A0E1A)
    val CharcoalBlack = Color(0xFF121824)
    val ElectricBlue = Color(0xFF007AFF)
    val ElectricBlueGlow = Color(0x66007AFF)
    val Cyan = Color(0xFF00E5FF)
    val CyanGlow = Color(0x6600E5FF)
    val Teal = Color(0xFF00BFA5)
    val PurpleAccent = Color(0xFF7C4DFF)
    val SoftWhite = Color(0xFFF0F4F8)
    val SoftWhiteTransparent = Color(0x80F0F4F8)
    val DimBackground = Color(0xCC0A0E1A)

    // Connection Type Colors
    val ConnectionDiscovery = Color(0x8800E5FF)
    val ConnectionConnected = Color(0xCC007AFF)
    val ConnectionRelay = Color(0xCC7C4DFF)
    val ConnectionBroadcast = Color(0xCC00BFA5)
    val ConnectionInactive = Color(0x223892FF)

    // Timing Durations
    const val STARTUP_ANIMATION_DURATION_MS = 2800L
    const val WELCOME_ANIMATION_DURATION_MS = 4800L

    // Phase Time Boundaries (Normalized 0.0f to 1.0f)
    const val PHASE_1_SILENCE_END = 0.10f
    const val PHASE_2_DISCOVERY_END = 0.25f
    const val PHASE_3_SCANNING_END = 0.40f
    const val PHASE_4_CONNECTION_END = 0.55f
    const val PHASE_5_SELF_HEALING_END = 0.70f
    const val PHASE_6_PACKET_ROUTING_END = 0.82f
    const val PHASE_7_USER_JOINS_END = 0.90f
    const val PHASE_8_LOGO_EMERGENCE_END = 0.96f
    const val PHASE_9_TRANSITION_END = 1.00f

    // Graph & Physics Parameters
    const val REGULAR_NODE_COUNT = 18
    const val AMBIENT_DUST_PARTICLE_COUNT = 55
    const val PACKET_COUNT = 16
    const val MAX_CONNECTIONS_PER_NODE = 4
    const val NODE_CONNECT_RADIUS_RATIO = 0.35f // Normalized distance threshold

    // Node Sizes
    const val MIN_NODE_RADIUS_DP = 4f
    const val MAX_NODE_RADIUS_DP = 9f
    const val USER_NODE_RADIUS_DP = 28f
}

/**
 * Visual classification of connection edges in the mesh network.
 */
enum class ConnectionType {
    DISCOVERY,
    CONNECTED,
    RELAY,
    BROADCAST,
    INACTIVE
}
