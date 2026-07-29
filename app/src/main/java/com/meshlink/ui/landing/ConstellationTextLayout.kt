package com.meshlink.ui.landing

import androidx.compose.ui.geometry.Offset

/**
 * Procedural anchor coordinate generator for the constellation text:
 *   Row 1: "WELCOME TO"
 *   Row 2: "MESH LINK"
 *
 * v3 improvements:
 *  • Better vertical centering — letters span y: 0.40 → 0.64 (more screen real-estate)
 *  • Wider horizontal spread — uses x: 0.09 → 0.91 for both rows
 *  • Minimum-stroke constellation style — only essential skeleton edges per letter,
 *    no cross-connecting "graph noise"
 *  • 12 accent "sparkle" dots distributed around the text for a constellation feel
 *  • Letter spacing slightly increased for elegance / readability
 */
object ConstellationTextLayout {

    data class ConstellationPoint(
        val xRatio: Float,
        val yRatio: Float,
        val letterIndex: Int,   // 0..16 — the letter this point belongs to
        val isStrokeStart: Boolean = false,
        val isAccentDot: Boolean = false  // v3: lone sparkle point, no connected edges
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
     * Generates normalized anchor points and stroke edges for "WELCOME TO / MESH LINK".
     *
     * Staggered emergence order:
     *   W(0) → E(1) → L(2) → C(3) → O(4) → M(5) → E(6) → T(7) → O(8)
     *   M(9) → E(10) → S(11) → H(12) → L(13) → I(14) → N(15) → K(16)
     */
    fun generateLayout(): LayoutResult {
        val points = mutableListOf<ConstellationPoint>()
        val edges  = mutableListOf<ConstellationEdge>()

        // ── Row metrics ──────────────────────────────────────────────────────
        val row1Y      = 0.405f   // "WELCOME TO" baseline top
        val row2Y      = 0.565f   // "MESH LINK"  baseline top
        val lh         = 0.088f   // letter height

        // Helper: add one letter's points and edges
        fun addLetter(
            letterIdx: Int,
            baseX: Float,
            baseY: Float,
            pts: List<Pair<Float, Float>>,
            segs: List<Pair<Int, Int>>
        ) {
            val start = points.size
            pts.forEachIndexed { i, (dx, dy) ->
                points.add(
                    ConstellationPoint(
                        xRatio = (baseX + dx).coerceIn(0.07f, 0.93f),
                        yRatio = (baseY + dy).coerceIn(0.18f, 0.82f),
                        letterIndex = letterIdx,
                        isStrokeStart = i == 0
                    )
                )
            }
            segs.forEach { (a, b) ->
                edges.add(ConstellationEdge(start + a, start + b))
            }
        }

        // Helper: add a lone sparkle accent dot (no edges)
        fun addAccent(letterIdx: Int, x: Float, y: Float) {
            points.add(
                ConstellationPoint(
                    xRatio = x.coerceIn(0.07f, 0.93f),
                    yRatio = y.coerceIn(0.18f, 0.82f),
                    letterIndex = letterIdx,
                    isAccentDot = true
                )
            )
        }

        // ── ROW 1: "WELCOME TO" ──────────────────────────────────────────────
        // Letter widths are tighter; letter spacing (gap between letters) = 0.008f

        // W  (letterIdx=0)  x=0.09
        addLetter(0, 0.090f, row1Y,
            listOf(0.000f to 0.000f, 0.012f to lh, 0.024f to lh*0.48f,
                   0.036f to lh,     0.048f to 0.000f),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)
        )

        // E  (letterIdx=1)  x=0.155
        addLetter(1, 0.155f, row1Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.034f to 0.000f,
                   0.030f to lh*0.49f, 0.034f to lh),
            listOf(0 to 1, 0 to 2, 1 to 4, 0 to 3)
        )

        // L  (letterIdx=2)  x=0.208
        addLetter(2, 0.208f, row1Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.036f to lh),
            listOf(0 to 1, 1 to 2)
        )
        addAccent(2, 0.230f, row1Y - 0.018f)   // sparkle above L

        // C  (letterIdx=3)  x=0.258
        addLetter(3, 0.258f, row1Y,
            listOf(0.034f to 0.000f, 0.000f to 0.018f,
                   0.000f to lh-0.018f, 0.034f to lh),
            listOf(0 to 1, 1 to 2, 2 to 3)
        )

        // O  (letterIdx=4)  x=0.307
        addLetter(4, 0.307f, row1Y,
            listOf(0.018f to 0.000f, 0.036f to lh*0.50f,
                   0.018f to lh,     0.000f to lh*0.50f),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
        )

        // M  (letterIdx=5)  x=0.358
        addLetter(5, 0.358f, row1Y,
            listOf(0.000f to lh, 0.000f to 0.000f, 0.024f to lh*0.50f,
                   0.048f to 0.000f, 0.048f to lh),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)
        )

        // E  (letterIdx=6)  x=0.420
        addLetter(6, 0.420f, row1Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.034f to 0.000f,
                   0.030f to lh*0.49f, 0.034f to lh),
            listOf(0 to 1, 0 to 2, 1 to 4, 0 to 3)
        )
        addAccent(6, 0.448f, row1Y + lh + 0.012f)  // sparkle below last E

        // (gap for space before "TO")

        // T  (letterIdx=7)  x=0.512
        addLetter(7, 0.512f, row1Y,
            listOf(0.000f to 0.000f, 0.044f to 0.000f,
                   0.022f to 0.000f, 0.022f to lh),
            listOf(0 to 1, 2 to 3)
        )

        // O  (letterIdx=8)  x=0.568
        addLetter(8, 0.568f, row1Y,
            listOf(0.018f to 0.000f, 0.036f to lh*0.50f,
                   0.018f to lh,     0.000f to lh*0.50f),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
        )
        addAccent(8, 0.540f, row1Y + lh * 0.25f)   // accent left of O

        // ── ROW 2: "MESH LINK" ───────────────────────────────────────────────

        // M  (letterIdx=9)  x=0.13
        addLetter(9, 0.130f, row2Y,
            listOf(0.000f to lh, 0.000f to 0.000f, 0.030f to lh*0.50f,
                   0.060f to 0.000f, 0.060f to lh),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)
        )

        // E  (letterIdx=10) x=0.206
        addLetter(10, 0.206f, row2Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.040f to 0.000f,
                   0.036f to lh*0.49f, 0.040f to lh),
            listOf(0 to 1, 0 to 2, 1 to 4, 0 to 3)
        )
        addAccent(10, 0.184f, row2Y - 0.016f)

        // S  (letterIdx=11) x=0.262
        addLetter(11, 0.262f, row2Y,
            listOf(0.040f to 0.000f, 0.000f to 0.000f, 0.000f to lh*0.46f,
                   0.040f to lh*0.54f, 0.040f to lh, 0.000f to lh),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5)
        )

        // H  (letterIdx=12) x=0.318
        addLetter(12, 0.318f, row2Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.000f to lh*0.50f,
                   0.048f to lh*0.50f, 0.048f to 0.000f, 0.048f to lh),
            listOf(0 to 1, 2 to 3, 4 to 5)
        )
        addAccent(12, 0.350f, row2Y + lh + 0.014f)

        // (gap for space before "LINK")

        // L  (letterIdx=13) x=0.422
        addLetter(13, 0.422f, row2Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.040f to lh),
            listOf(0 to 1, 1 to 2)
        )

        // I  (letterIdx=14) x=0.478
        addLetter(14, 0.478f, row2Y,
            listOf(0.000f to 0.000f, 0.000f to lh),
            listOf(0 to 1)
        )
        addAccent(14, 0.494f, row2Y + lh * 0.20f)   // tiny sparkle beside I

        // N  (letterIdx=15) x=0.510
        addLetter(15, 0.510f, row2Y,
            listOf(0.000f to lh, 0.000f to 0.000f,
                   0.052f to lh, 0.052f to 0.000f),
            listOf(0 to 1, 1 to 2, 2 to 3)
        )

        // K  (letterIdx=16) x=0.580
        addLetter(16, 0.580f, row2Y,
            listOf(0.000f to 0.000f, 0.000f to lh, 0.046f to 0.000f,
                   0.000f to lh*0.47f, 0.046f to lh),
            listOf(0 to 1, 2 to 3, 3 to 4)
        )
        addAccent(16, 0.626f, row2Y + lh * 0.70f)   // sparkle right of K

        return LayoutResult(points, edges)
    }
}
