package com.meshlink.ui.landing

import androidx.compose.ui.geometry.Offset

/**
 * Procedural anchor coordinate generator for the constellation text:
 * "WELCOME TO" (Row 1)
 * "MESH LINK" (Row 2)
 *
 * Provides normalized (x, y) coordinates in range [0.1..0.9] for star node migration,
 * along with letter stroke edge definitions for constellation line connections.
 */
object ConstellationTextLayout {

    data class ConstellationPoint(
        val xRatio: Float,
        val yRatio: Float,
        val letterIndex: Int, // 0..16 representing letter order
        val isStrokeStart: Boolean = false
    )

    data class ConstellationEdge(
        val pointIndexA: Int,
        val pointIndexB: Int
    )

    data class LayoutResult(
        val points: List<ConstellationPoint>,
        val edges: List<ConstellationEdge>
    )

    /**
     * Generates normalized anchor points and stroke edges for "WELCOME TO \n MESH LINK".
     * Staggered emergence order:
     * W (0) -> E (1) -> L (2) -> C (3) -> O (4) -> M (5) -> E (6) -> T (7) -> O (8)
     * M (9) -> E (10) -> S (11) -> H (12) -> L (13) -> I (14) -> N (15) -> K (16)
     */
    fun generateLayout(): LayoutResult {
        val points = mutableListOf<ConstellationPoint>()
        val edges = mutableListOf<ConstellationEdge>()

        // Row 1: "WELCOME TO" at yRatio ~ 0.38f
        // Row 2: "MESH LINK"  at yRatio ~ 0.54f

        val row1Y = 0.38f
        val row2Y = 0.54f
        val letterHeight = 0.07f

        // Helper to add a letter with stroke points
        fun addLetterPoints(
            letterIdx: Int,
            baseX: Float,
            baseY: Float,
            strokePoints: List<Pair<Float, Float>>,
            strokeEdges: List<Pair<Int, Int>>
        ): Int {
            val startIndex = points.size
            strokePoints.forEachIndexed { idx, (dx, dy) ->
                points.add(
                    ConstellationPoint(
                        xRatio = (baseX + dx).coerceIn(0.08f, 0.92f),
                        yRatio = (baseY + dy).coerceIn(0.15f, 0.85f),
                        letterIndex = letterIdx,
                        isStrokeStart = idx == 0
                    )
                )
            }
            strokeEdges.forEach { (a, b) ->
                edges.add(ConstellationEdge(startIndex + a, startIndex + b))
            }
            return points.size
        }

        // --- ROW 1: "WELCOME TO" ---
        // Letter 0: W (x: 0.11)
        addLetterPoints(
            letterIdx = 0, baseX = 0.11f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.01f, letterHeight), Pair(0.02f, letterHeight * 0.5f),
                Pair(0.03f, letterHeight), Pair(0.04f, 0.00f)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4))
        )

        // Letter 1: E (x: 0.18)
        addLetterPoints(
            letterIdx = 1, baseX = 0.18f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.03f, 0.00f),
                Pair(0.025f, letterHeight * 0.5f), Pair(0.03f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(0, 2), Pair(1, 4), Pair(0, 3))
        )

        // Letter 2: L (x: 0.24)
        addLetterPoints(
            letterIdx = 2, baseX = 0.24f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.03f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2))
        )

        // Letter 3: C (x: 0.30)
        addLetterPoints(
            letterIdx = 3, baseX = 0.30f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.03f, 0.00f), Pair(0.00f, 0.02f), Pair(0.00f, letterHeight - 0.02f), Pair(0.03f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3))
        )

        // Letter 4: O (x: 0.36)
        addLetterPoints(
            letterIdx = 4, baseX = 0.36f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.015f, 0.00f), Pair(0.03f, letterHeight * 0.5f),
                Pair(0.015f, letterHeight), Pair(0.00f, letterHeight * 0.5f)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0))
        )

        // Letter 5: M (x: 0.42)
        addLetterPoints(
            letterIdx = 5, baseX = 0.42f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.00f, letterHeight), Pair(0.00f, 0.00f), Pair(0.02f, letterHeight * 0.5f),
                Pair(0.04f, 0.00f), Pair(0.04f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4))
        )

        // Letter 6: E (x: 0.49)
        addLetterPoints(
            letterIdx = 6, baseX = 0.49f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.03f, 0.00f),
                Pair(0.025f, letterHeight * 0.5f), Pair(0.03f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(0, 2), Pair(1, 4), Pair(0, 3))
        )

        // Space before "TO"
        // Letter 7: T (x: 0.60)
        addLetterPoints(
            letterIdx = 7, baseX = 0.60f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.04f, 0.00f), Pair(0.02f, 0.00f), Pair(0.02f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(2, 3))
        )

        // Letter 8: O (x: 0.67)
        addLetterPoints(
            letterIdx = 8, baseX = 0.67f, baseY = row1Y,
            strokePoints = listOf(
                Pair(0.015f, 0.00f), Pair(0.03f, letterHeight * 0.5f),
                Pair(0.015f, letterHeight), Pair(0.00f, letterHeight * 0.5f)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0))
        )

        // --- ROW 2: "MESH LINK" ---
        // Letter 9: M (x: 0.17)
        addLetterPoints(
            letterIdx = 9, baseX = 0.17f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, letterHeight), Pair(0.00f, 0.00f), Pair(0.025f, letterHeight * 0.5f),
                Pair(0.05f, 0.00f), Pair(0.05f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4))
        )

        // Letter 10: E (x: 0.25)
        addLetterPoints(
            letterIdx = 10, baseX = 0.25f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.035f, 0.00f),
                Pair(0.03f, letterHeight * 0.5f), Pair(0.035f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(0, 2), Pair(1, 4), Pair(0, 3))
        )

        // Letter 11: S (x: 0.32)
        addLetterPoints(
            letterIdx = 11, baseX = 0.32f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.035f, 0.00f), Pair(0.00f, 0.00f), Pair(0.00f, letterHeight * 0.45f),
                Pair(0.035f, letterHeight * 0.55f), Pair(0.035f, letterHeight), Pair(0.00f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4), Pair(4, 5))
        )

        // Letter 12: H (x: 0.39)
        addLetterPoints(
            letterIdx = 12, baseX = 0.39f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.00f, letterHeight * 0.5f),
                Pair(0.04f, letterHeight * 0.5f), Pair(0.04f, 0.00f), Pair(0.04f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(2, 3), Pair(4, 5))
        )

        // Space before "LINK"
        // Letter 13: L (x: 0.50)
        addLetterPoints(
            letterIdx = 13, baseX = 0.50f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.035f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2))
        )

        // Letter 14: I (x: 0.57)
        addLetterPoints(
            letterIdx = 14, baseX = 0.57f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1))
        )

        // Letter 15: N (x: 0.61)
        addLetterPoints(
            letterIdx = 15, baseX = 0.61f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, letterHeight), Pair(0.00f, 0.00f), Pair(0.045f, letterHeight), Pair(0.045f, 0.00f)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(1, 2), Pair(2, 3))
        )

        // Letter 16: K (x: 0.69)
        addLetterPoints(
            letterIdx = 16, baseX = 0.69f, baseY = row2Y,
            strokePoints = listOf(
                Pair(0.00f, 0.00f), Pair(0.00f, letterHeight), Pair(0.04f, 0.00f),
                Pair(0.00f, letterHeight * 0.45f), Pair(0.04f, letterHeight)
            ),
            strokeEdges = listOf(Pair(0, 1), Pair(2, 3), Pair(3, 4))
        )

        return LayoutResult(points, edges)
    }
}
