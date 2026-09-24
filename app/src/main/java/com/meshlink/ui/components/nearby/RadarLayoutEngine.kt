package com.meshlink.ui.components.nearby

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.meshlink.core.data.UserRepositoryImpl
import com.meshlink.domain.model.BleDevice
import com.meshlink.util.MeshIdNormalizer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class LabelPlacement {
    TOP, BOTTOM, LEFT, RIGHT
}

data class RadarNodeLayout(
    val device: BleDevice,
    val canonicalId: String,
    val displayName: String,
    val initial: String,
    val centerOffset: Offset,
    val nodeRadiusPx: Float,
    val labelTopLeft: Offset,
    val labelSize: Size,
    val labelPlacement: LabelPlacement,
    val nameLayoutResult: TextLayoutResult?,
    val initialLayoutResult: TextLayoutResult?,
    val isConnected: Boolean,
    val isSelected: Boolean,
    val isMatchSearch: Boolean,
    val showLabel: Boolean
)

data class RadarSceneLayout(
    val center: Offset,
    val maxRadius: Float,
    val centerNodeRadiusPx: Float,
    val youLabelRect: Rect,
    val youLayoutResult: TextLayoutResult?,
    val nodes: List<RadarNodeLayout>
)

object RadarLayoutEngine {

    /**
     * Resolves a verified profile display name, falling back to neutral "Mesh Peer".
     * Never exposes smartphone hardware or Bluetooth device models.
     */
    fun resolveDisplayName(device: BleDevice, canonicalId: String): String {
        val profName = device.displayName?.trim()?.takeIf {
            it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
        }
        val fallbackName = device.name.trim().takeIf {
            it.isNotBlank() && !UserRepositoryImpl.isGenericOrInvalidName(it, canonicalId)
        }
        return profName ?: fallbackName ?: "Mesh Peer"
    }

    /**
     * Extracts an initial character for the peer marker badge (e.g. "D", "M").
     */
    fun extractInitial(displayName: String): String {
        val trimmed = displayName.trim()
        val firstChar = trimmed.firstOrNull { it.isLetterOrDigit() } ?: 'M'
        return firstChar.uppercase()
    }

    /**
     * Computes a stable base angle strictly from the canonical Mesh ID hash.
     * Guaranteed to match test14 in NearbyDiscoveryPipelineTest.
     */
    fun computeBaseAngleDeg(canonicalId: String): Float {
        val idHash = abs((canonicalId.hashCode().toLong() and 0xFFFFFFFFL))
        return (idHash % 360).toFloat()
    }

    /**
     * Computes the complete deterministic, collision-aware scene layout.
     * Invoked once per dataset change outside the animation frame loop.
     */
    fun calculateLayout(
        devices: List<BleDevice>,
        canvasSize: Size,
        density: Density,
        textMeasurer: TextMeasurer? = null,
        textSizeEstimator: ((String, Boolean) -> Size)? = null,
        labelTextStyle: TextStyle = TextStyle.Default,
        initialTextStyle: TextStyle = TextStyle.Default,
        youTextStyle: TextStyle = TextStyle.Default,
        selectedAddress: String? = null,
        searchQuery: String = ""
    ): RadarSceneLayout {
        val width = canvasSize.width
        val height = canvasSize.height
        val center = Offset(width / 2f, height / 2f)
        val maxRadius = (min(width, height) / 2f) * 0.90f

        if (maxRadius <= 0f) {
            val emptyYouLayout = textMeasurer?.measure(text = "You", style = youTextStyle)
            return RadarSceneLayout(
                center = center,
                maxRadius = 0f,
                centerNodeRadiusPx = 0f,
                youLabelRect = Rect.Zero,
                youLayoutResult = emptyYouLayout,
                nodes = emptyList()
            )
        }

        // Center "You" node dimensions
        val centerNodeRadiusPx = with(density) { 15.dp.toPx() }
        val youLayoutResult = textMeasurer?.measure(text = "You", style = youTextStyle)
        val youMeasuredSize = youLayoutResult?.let { Size(it.size.width.toFloat(), it.size.height.toFloat()) }
            ?: textSizeEstimator?.invoke("You", false)
            ?: Size(40f, 18f)

        val youPillPaddingH = with(density) { 6.dp.toPx() }
        val youPillPaddingV = with(density) { 1.5.dp.toPx() }
        val youPillWidth = youMeasuredSize.width + youPillPaddingH * 2
        val youPillHeight = youMeasuredSize.height + youPillPaddingV * 2
        val youPillTop = center.y + centerNodeRadiusPx + with(density) { 2.dp.toPx() }
        val youPillLeft = center.x - youPillWidth / 2f
        val youLabelRect = Rect(
            left = youPillLeft,
            top = youPillTop,
            right = youPillLeft + youPillWidth,
            bottom = youPillTop + youPillHeight
        )

        // Strict center exclusion zone covering center node + "You" pill
        val minCenterExclusion = with(density) {
            max(42.dp.toPx(), centerNodeRadiusPx + youPillHeight + 8.dp.toPx())
        }
        val peerNodeRadiusPx = with(density) { 11.dp.toPx() }
        val minNodeSeparation = with(density) { 34.dp.toPx() }
        val outerMargin = with(density) { 8.dp.toPx() }
        val outerBoundary = max(minCenterExclusion + 10f, maxRadius - outerMargin)

        // Deduplicate devices by canonical ID to avoid multi-transport ghosting
        val canonicalMap = LinkedHashMap<String, BleDevice>()
        for (dev in devices) {
            val cid = MeshIdNormalizer.canonicalize(dev.meshId.ifBlank { dev.address })
            if (cid.isNotBlank()) {
                val existing = canonicalMap[cid]
                if (existing == null || (dev.isConnected && !existing.isConnected)) {
                    canonicalMap[cid] = dev
                }
            }
        }

        // Sort deterministically by canonicalId so placement order is totally stable
        val sortedPeers = canonicalMap.entries.sortedBy { it.key }

        val placedNodes = ArrayList<RadarNodeLayout>(sortedPeers.size)
        val placedNodeCenters = ArrayList<Offset>(sortedPeers.size)
        val placedLabelRects = ArrayList<Rect>(sortedPeers.size)

        val angleOffsets = intArrayOf(
            0, 18, -18, 36, -36, 54, -54, 72, -72, 90, -90, 110, -110, 130, -130, 150, -150, 180
        )
        val radiusScaleOptions = floatArrayOf(1.0f, 0.85f, 1.15f)

        val totalPeers = sortedPeers.size
        val highDensity = totalPeers >= 8

        for ((canonicalId, device) in sortedPeers) {
            val displayName = resolveDisplayName(device, canonicalId)
            val initial = extractInitial(displayName)
            val isSelected = device.address == selectedAddress
            val isDirectConnected = device.isConnected
            val isMatchSearch = searchQuery.isBlank() ||
                displayName.contains(searchQuery, ignoreCase = true) ||
                canonicalId.contains(searchQuery, ignoreCase = true)

            val baseAngleDeg = computeBaseAngleDeg(canonicalId)

            // Quantized radial bands to prevent micro-jitter
            val baseRadiusRatio = when {
                device.isMeshNode -> 0.94f
                device.rssi >= -65 -> 0.35f  // Strong signal
                device.rssi >= -80 -> 0.65f  // Medium signal
                else -> 0.90f                // Weak signal
            }

            val rBase = minCenterExclusion + (outerBoundary - minCenterExclusion) * baseRadiusRatio

            // Collision-aware position search
            var bestPos: Offset? = null
            var maxMinDist = -1f

            searchLoop@ for (scale in radiusScaleOptions) {
                val rCandidate = (rBase * scale).coerceIn(minCenterExclusion + with(density) { 4.dp.toPx() }, outerBoundary)
                for (aOffset in angleOffsets) {
                    val angleDeg = (baseAngleDeg + aOffset + 360f) % 360f
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                    val posX = center.x + rCandidate * cos(angleRad)
                    val posY = center.y + rCandidate * sin(angleRad)
                    val candidatePos = Offset(posX, posY)

                    // Safe canvas bounds check
                    if (posX < peerNodeRadiusPx + outerMargin || posX > width - peerNodeRadiusPx - outerMargin ||
                        posY < peerNodeRadiusPx + outerMargin || posY > height - peerNodeRadiusPx - outerMargin) {
                        continue
                    }

                    // Check center exclusion
                    val distToCenter = distance(candidatePos, center)
                    if (distToCenter < minCenterExclusion) {
                        continue
                    }

                    // Check collision against "You" label rect
                    if (circleIntersectsRect(candidatePos, peerNodeRadiusPx + 2f, youLabelRect)) {
                        continue
                    }

                    // Check separation from already placed nodes
                    var collidesWithPeer = false
                    var minPeerDist = Float.MAX_VALUE
                    for (placedCenter in placedNodeCenters) {
                        val d = distance(candidatePos, placedCenter)
                        if (d < minPeerDist) minPeerDist = d
                        if (d < minNodeSeparation) {
                            collidesWithPeer = true
                            break
                        }
                    }

                    if (!collidesWithPeer) {
                        bestPos = candidatePos
                        break@searchLoop
                    } else if (minPeerDist > maxMinDist) {
                        maxMinDist = minPeerDist
                        bestPos = candidatePos
                    }
                }
            }

            val nodePos = bestPos ?: Offset(
                center.x + rBase * cos(Math.toRadians(baseAngleDeg.toDouble()).toFloat()),
                center.y + rBase * sin(Math.toRadians(baseAngleDeg.toDouble()).toFloat())
            )

            // Text Layout Results for this node
            val truncatedName = when {
                highDensity && displayName.length > 8 -> displayName.take(7) + "…"
                displayName.length > 12 -> displayName.take(11) + "…"
                else -> displayName
            }

            val nameLayoutResult = textMeasurer?.measure(
                text = truncatedName,
                style = labelTextStyle
            )
            val initialLayoutResult = textMeasurer?.measure(
                text = initial,
                style = initialTextStyle
            )

            val nameMeasuredSize = nameLayoutResult?.let { Size(it.size.width.toFloat(), it.size.height.toFloat()) }
                ?: textSizeEstimator?.invoke(truncatedName, false)
                ?: Size(truncatedName.length * 10f, 16f)

            val pillPadH = with(density) { 5.dp.toPx() }
            val pillPadV = with(density) { 2.dp.toPx() }
            val labelW = nameMeasuredSize.width + pillPadH * 2
            val labelH = nameMeasuredSize.height + pillPadV * 2
            val labelSize = Size(labelW, labelH)
            val gap = with(density) { 3.dp.toPx() }

            // Candidate label placements in preferred outward order
            val dxFromCenter = nodePos.x - center.x
            val dyFromCenter = nodePos.y - center.y

            val placementCandidates = ArrayList<LabelPlacement>(4)
            if (abs(dyFromCenter) >= abs(dxFromCenter)) {
                // More vertical offset
                if (dyFromCenter < 0) {
                    placementCandidates.add(LabelPlacement.TOP)
                    placementCandidates.add(if (dxFromCenter >= 0) LabelPlacement.RIGHT else LabelPlacement.LEFT)
                    placementCandidates.add(if (dxFromCenter >= 0) LabelPlacement.LEFT else LabelPlacement.RIGHT)
                    placementCandidates.add(LabelPlacement.BOTTOM)
                } else {
                    placementCandidates.add(LabelPlacement.BOTTOM)
                    placementCandidates.add(if (dxFromCenter >= 0) LabelPlacement.RIGHT else LabelPlacement.LEFT)
                    placementCandidates.add(if (dxFromCenter >= 0) LabelPlacement.LEFT else LabelPlacement.RIGHT)
                    placementCandidates.add(LabelPlacement.TOP)
                }
            } else {
                // More horizontal offset
                if (dxFromCenter >= 0) {
                    placementCandidates.add(LabelPlacement.RIGHT)
                    placementCandidates.add(if (dyFromCenter < 0) LabelPlacement.TOP else LabelPlacement.BOTTOM)
                    placementCandidates.add(if (dyFromCenter < 0) LabelPlacement.BOTTOM else LabelPlacement.TOP)
                    placementCandidates.add(LabelPlacement.LEFT)
                } else {
                    placementCandidates.add(LabelPlacement.LEFT)
                    placementCandidates.add(if (dyFromCenter < 0) LabelPlacement.TOP else LabelPlacement.BOTTOM)
                    placementCandidates.add(if (dyFromCenter < 0) LabelPlacement.BOTTOM else LabelPlacement.TOP)
                    placementCandidates.add(LabelPlacement.RIGHT)
                }
            }

            var chosenPlacement = placementCandidates.first()
            var chosenLabelTopLeft = computeLabelTopLeft(nodePos, peerNodeRadiusPx, gap, labelSize, chosenPlacement)
            var chosenRect = Rect(chosenLabelTopLeft, labelSize)
            var foundNonColliding = false

            for (placement in placementCandidates) {
                val candidateTopLeft = computeLabelTopLeft(nodePos, peerNodeRadiusPx, gap, labelSize, placement)
                // Clamp within canvas bounds
                val clampedX = candidateTopLeft.x.coerceIn(outerMargin, width - labelW - outerMargin)
                val clampedY = candidateTopLeft.y.coerceIn(outerMargin, height - labelH - outerMargin)
                val candidateRect = Rect(Offset(clampedX, clampedY), labelSize)

                // Check collision against center "You" zone
                if (candidateRect.overlaps(youLabelRect)) continue
                if (circleIntersectsRect(center, centerNodeRadiusPx + 2f, candidateRect)) continue

                // Check collision against other node circles
                var overlapsAnyNode = false
                for (placedCenter in placedNodeCenters) {
                    if (circleIntersectsRect(placedCenter, peerNodeRadiusPx + 2f, candidateRect)) {
                        overlapsAnyNode = true
                        break
                    }
                }
                if (overlapsAnyNode) continue

                // Check collision against placed label rects
                var overlapsAnyLabel = false
                for (placedRect in placedLabelRects) {
                    if (candidateRect.overlaps(placedRect)) {
                        overlapsAnyLabel = true
                        break
                    }
                }
                if (overlapsAnyLabel) continue

                // Non-colliding position found!
                chosenPlacement = placement
                chosenLabelTopLeft = Offset(clampedX, clampedY)
                chosenRect = candidateRect
                foundNonColliding = true
                break
            }

            // In extreme density, if no non-colliding label position was found and peer count >= 7,
            // hide label by default to prevent visual cluster (can still be viewed on selection or tap)
            val showLabel = if (foundNonColliding) {
                true
            } else if (totalPeers <= 5 || isSelected) {
                // Clamp best placement anyway
                val clampedX = chosenLabelTopLeft.x.coerceIn(outerMargin, width - labelW - outerMargin)
                val clampedY = chosenLabelTopLeft.y.coerceIn(outerMargin, height - labelH - outerMargin)
                chosenLabelTopLeft = Offset(clampedX, clampedY)
                chosenRect = Rect(chosenLabelTopLeft, labelSize)
                true
            } else {
                false
            }

            placedNodeCenters.add(nodePos)
            if (showLabel) {
                placedLabelRects.add(chosenRect)
            }

            placedNodes.add(
                RadarNodeLayout(
                    device = device,
                    canonicalId = canonicalId,
                    displayName = displayName,
                    initial = initial,
                    centerOffset = nodePos,
                    nodeRadiusPx = peerNodeRadiusPx,
                    labelTopLeft = chosenLabelTopLeft,
                    labelSize = labelSize,
                    labelPlacement = chosenPlacement,
                    nameLayoutResult = nameLayoutResult,
                    initialLayoutResult = initialLayoutResult,
                    isConnected = isDirectConnected,
                    isSelected = isSelected,
                    isMatchSearch = isMatchSearch,
                    showLabel = showLabel
                )
            )
        }

        return RadarSceneLayout(
            center = center,
            maxRadius = maxRadius,
            centerNodeRadiusPx = centerNodeRadiusPx,
            youLabelRect = youLabelRect,
            youLayoutResult = youLayoutResult,
            nodes = placedNodes
        )
    }

    private fun computeLabelTopLeft(
        nodePos: Offset,
        nodeRadius: Float,
        gap: Float,
        labelSize: Size,
        placement: LabelPlacement
    ): Offset {
        return when (placement) {
            LabelPlacement.BOTTOM -> Offset(nodePos.x - labelSize.width / 2f, nodePos.y + nodeRadius + gap)
            LabelPlacement.TOP -> Offset(nodePos.x - labelSize.width / 2f, nodePos.y - nodeRadius - gap - labelSize.height)
            LabelPlacement.RIGHT -> Offset(nodePos.x + nodeRadius + gap, nodePos.y - labelSize.height / 2f)
            LabelPlacement.LEFT -> Offset(nodePos.x - nodeRadius - gap - labelSize.width, nodePos.y - labelSize.height / 2f)
        }
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun circleIntersectsRect(circleCenter: Offset, circleRadius: Float, rect: Rect): Boolean {
        val closestX = circleCenter.x.coerceIn(rect.left, rect.right)
        val closestY = circleCenter.y.coerceIn(rect.top, rect.bottom)
        val dx = circleCenter.x - closestX
        val dy = circleCenter.y - closestY
        return (dx * dx + dy * dy) < (circleRadius * circleRadius)
    }
}
