package com.meshlink.ui.landing

import kotlin.math.hypot

/**
 * Particle / ephemeral-effect data classes — v4.
 *
 * Changes from v3:
 *  1. [DataPacket.isReversing] — when true, the packet's progress counts DOWN
 *     (converging toward the reversal target node = Mesh Link signature moment).
 *  2. [RadialSignatureWave.trigger] gains optional [jitterX]/[jitterY] parameters
 *     so the wave origin is randomized within ±SIGNATURE_ORIGIN_JITTER each launch.
 *  3. NEW [HeartbeatWave] — graph-propagated wave separate from the visual ring.
 *     It sets [AnimatedMeshNode.signatureWaveFlash] as the wavefront arrives at
 *     each node, producing an organic staggered acknowledgement.
 *  4. [MicroConnection] and [ConstellationRippleWave] unchanged.
 */

// ── Data Packet ───────────────────────────────────────────────────────────────

class DataPacket(
    val id: Int,
    var fromNodeId: Int,
    var toNodeId: Int,
    val baseSpeed: Float = 0.55f,
    val delayProgress: Float = 0.30f,
    val sizeDp: Float = 2.8f,
    val willDisappear: Boolean = false
) {
    var isActive       = false
    var progress       = 0f
    var currentSpeed   = baseSpeed
    var currentX       = 0f
    var currentY       = 0f
    var state          = PacketState.TRAVELING
    var pauseTimerSec  = 0f
    var fadeAlpha      = 1.0f

    // v4: reversal support
    /** When true the packet travels backwards (progress counts down → 0) converging on
     *  the reversal target. Set by [MeshConnectionAnimator] at PACKET_REVERSAL_START. */
    var isReversing    = false
    /** Speed multiplier applied during reversal to make convergence feel urgent. */
    var reversalSpeedMult = 1.8f

    fun resetForNextHop() {
        progress       = 0f
        isReversing    = false
        state          = PacketState.TRAVELING
    }
}

enum class PacketState {
    TRAVELING,
    PAUSED_AT_RELAY,
    FADING_OUT,
    CONVERGING   // v3: used during Scene 9 convergence
}

// ── Micro-connection (background) ─────────────────────────────────────────────

class MicroConnection(
    val id: Int,
    var nodeAId: Int,
    var nodeBId: Int
) {
    var isActive        = false
    var ageSec          = 0f
    var lifespanSec     = 2.0f
    var maxAlpha        = 0.15f
    var currentAlpha    = 0f
    var flashBrightness = 0f

    @Suppress("UNUSED_PARAMETER")
    fun reset(nA: Int, nB: Int, lifespan: Float, alpha: Float) {
        ageSec          = 0f
        lifespanSec     = lifespan
        maxAlpha        = alpha
        currentAlpha    = 0f
        isActive        = true
        flashBrightness = 0f
    }

    fun update(deltaSec: Float) {
        if (!isActive) return
        ageSec += deltaSec
        val t = (ageSec / lifespanSec).coerceIn(0f, 1f)
        // Fade-in first 20%, constant for 60%, fade-out last 20%
        currentAlpha = maxAlpha * when {
            t < 0.20f -> t / 0.20f
            t < 0.80f -> 1.0f
            else      -> (1.0f - t) / 0.20f
        }
        if (ageSec >= lifespanSec) { isActive = false; currentAlpha = 0f }
        flashBrightness = (flashBrightness - deltaSec * 2.5f).coerceAtLeast(0f)
    }
}

// ── Radial Signature Wave (visual ring) ───────────────────────────────────────

/**
 * Visual expanding ring originating from a jittered corner position.
 * Provides the "shockwave" visual that radiates across the universe.
 * Per-node flash is now handled separately by [HeartbeatWave].
 */
class RadialSignatureWave {
    var isActive   = false
    var radius     = 0f          // 0→1 (relative to screen diagonal)
    var alpha      = 0f
    var speed      = 0.55f       // screen diagonals per second
    var originX    = AnimationConstants.SIGNATURE_ORIGIN_X_BASE
    var originY    = AnimationConstants.SIGNATURE_ORIGIN_Y_BASE

    fun trigger(
        jitterX: Float = 0f,
        jitterY: Float = 0f
    ) {
        isActive = true
        radius   = 0f
        alpha    = 0.90f
        originX  = (AnimationConstants.SIGNATURE_ORIGIN_X_BASE + jitterX)
            .coerceIn(0.05f, 0.30f)
        originY  = (AnimationConstants.SIGNATURE_ORIGIN_Y_BASE + jitterY)
            .coerceIn(0.72f, 0.95f)
    }

    fun update(deltaSec: Float) {
        if (!isActive) return
        radius += deltaSec * speed
        // Alpha fades after the ring has expanded past 40% of the screen
        alpha  = (1.0f - (radius / 1.2f)).coerceIn(0f, 0.90f)
        if (radius > 1.3f) { isActive = false; alpha = 0f }
    }

    /** Returns 0→1 overlap intensity for a point at normalized screen coordinates. */
    fun nodeOverlap(nx: Float, ny: Float): Float {
        val dx   = nx - originX
        val dy   = ny - originY
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val diff = kotlin.math.abs(dist - radius)
        return if (diff < 0.045f) (1f - diff / 0.045f) * alpha else 0f
    }
}

// ── Heartbeat Wave (graph-propagated, v4) ─────────────────────────────────────

/**
 * The mesh heartbeat wave propagates from one relay hub through the connection graph,
 * arriving at each node with a delay proportional to its hop distance.
 *
 * When the wave arrives at a node, [onNodeReached] is called with that node's id
 * and the animator sets [AnimatedMeshNode.signatureWaveFlash] = 1f on it.
 */
class HeartbeatWave {
    var isActive      = false
    var originNodeId  = -1
    var elapsedSec    = 0f

    // Map<nodeId, arrivalTimeSec> — pre-computed during trigger()
    private val arrivalTimes = mutableMapOf<Int, Float>()

    /** Callback invoked (once) when the wave front reaches a node. */
    var onNodeReached: ((nodeId: Int) -> Unit)? = null

    private val alreadyFired = mutableSetOf<Int>()

    /**
     * Pre-compute arrival times via BFS over the connection graph.
     * [links] is the list of all active [MeshConnectionLink] objects.
     */
    fun trigger(
        originNodeId: Int,
        nodes: List<AnimatedMeshNode>,
        links: List<MeshConnectionLink>
    ) {
        this.originNodeId = originNodeId
        this.isActive     = true
        this.elapsedSec   = 0f
        arrivalTimes.clear()
        alreadyFired.clear()

        // Build adjacency from active links
        val adj = mutableMapOf<Int, MutableList<Pair<Int, Float>>>()
        links.forEach { link ->
            if (link.alpha > 0.1f) {
                adj.getOrPut(link.nodeA.id) { mutableListOf() }.add(link.nodeB.id to link.distance)
                adj.getOrPut(link.nodeB.id) { mutableListOf() }.add(link.nodeA.id to link.distance)
            }
        }

        // BFS with distance-weighted delay: each hop adds 0.018s + dist * 0.06s
        val queue = ArrayDeque<Pair<Int, Float>>()
        arrivalTimes[originNodeId] = 0f
        queue.add(originNodeId to 0f)

        while (queue.isNotEmpty()) {
            val (nodeId, arrivalTime) = queue.removeFirst()
            adj[nodeId]?.forEach { (neighborId, dist) ->
                if (!arrivalTimes.containsKey(neighborId)) {
                    val neighborArrival = arrivalTime + 0.018f + dist * 0.055f
                    arrivalTimes[neighborId] = neighborArrival
                    queue.add(neighborId to neighborArrival)
                }
            }
        }

        // Nodes not reachable via graph get a fallback based on spatial distance
        val origin = nodes.firstOrNull { it.id == originNodeId }
        if (origin != null) {
            nodes.forEach { n ->
                if (!arrivalTimes.containsKey(n.id)) {
                    val dx = n.startXRatio - origin.startXRatio
                    val dy = n.startYRatio - origin.startYRatio
                    arrivalTimes[n.id] = hypot(dx.toDouble(), dy.toDouble()).toFloat() * 0.5f + 0.05f
                }
            }
        }
    }

    fun update(deltaSec: Float, nodes: List<AnimatedMeshNode>) {
        if (!isActive) return
        elapsedSec += deltaSec

        nodes.forEach { node ->
            val arrival = arrivalTimes[node.id] ?: return@forEach
            if (elapsedSec >= arrival && !alreadyFired.contains(node.id)) {
                alreadyFired.add(node.id)
                node.signatureWaveFlash = 1f
                onNodeReached?.invoke(node.id)
            }
        }

        // Wave completes when elapsed > max arrival time + tail buffer
        val maxArrival = arrivalTimes.values.maxOrNull() ?: 0f
        if (elapsedSec > maxArrival + 0.4f) isActive = false
    }

    fun reset() {
        isActive = false
        arrivalTimes.clear()
        alreadyFired.clear()
        elapsedSec   = 0f
        originNodeId = -1
    }
}

// ── Constellation ripple wave (legacy) ───────────────────────────────────────

class ConstellationRippleWave {
    var isActive    = false
    var radius      = 0f
    var alpha       = 0f
    var centerX     = 0.5f
    var centerY     = 0.46f
    var isEcho      = false
    private var speed = 0.60f

    fun trigger(cx: Float = 0.5f, cy: Float = 0.46f, isEchoWave: Boolean = false) {
        isActive = true
        radius   = 0f
        alpha    = if (isEchoWave) 0.45f else 0.70f
        centerX  = cx
        centerY  = cy
        isEcho   = isEchoWave
        speed    = if (isEchoWave) 0.45f else 0.60f
    }

    fun update(deltaSec: Float) {
        if (!isActive) return
        radius += deltaSec * speed
        alpha   = (alpha - deltaSec * 0.90f).coerceAtLeast(0f)
        if (alpha <= 0f) { isActive = false; radius = 0f }
    }
}
