package com.meshlink.ble.discovery

/**
 * Calculates a dynamic score (0-100) for a peer to determine connection priority.
 */
object PeerScoreCalculator {

    /**
     * @param smoothedRssi Smoothed RSSI value (e.g., from RssiFilter).
     * @param failedAttempts Number of consecutive failed connection attempts.
     * @param lastSeenMillis The System.currentTimeMillis() when the peer was last seen.
     * @param hopCount (Optional) Hop count to destination, if known. 0 = direct.
     * @return An integer between 0 and 100. Higher is better.
     */
    fun calculateScore(
        smoothedRssi: Int,
        failedAttempts: Int,
        lastSeenMillis: Long,
        hopCount: Int = 0
    ): Int {
        var score = 0f

        // 1. RSSI Score (Weight: 50%)
        // Typical BLE RSSI ranges from -100 (weak) to -40 (strong)
        val clampedRssi = smoothedRssi.coerceIn(-100, -40).toFloat()
        val rssiNormalized = (clampedRssi + 100f) / 60f // 0.0 to 1.0
        score += rssiNormalized * 50f

        // 2. Stability/Failures Penalty
        // Each failure reduces score significantly. > 5 failures practically zeros the stability score.
        val failurePenalty = (failedAttempts * 10f).coerceAtMost(30f)
        score -= failurePenalty

        // 3. Staleness Penalty
        // If we haven't seen an advertisement recently, downgrade priority.
        val ageSeconds = (System.currentTimeMillis() - lastSeenMillis) / 1000f
        val stalenessPenalty = if (ageSeconds > 10f) {
            // Subtract 2 points for every second past 10s, max 20 points
            ((ageSeconds - 10f) * 2f).coerceAtMost(20f)
        } else {
            0f
        }
        score -= stalenessPenalty

        // 4. Hop Count Bonus/Penalty (Weight: 20%)
        // Prefer direct connections (0 hops) or low hops.
        val hopBonus = when (hopCount) {
            0 -> 20f
            1 -> 10f
            2 -> 5f
            else -> 0f
        }
        score += hopBonus
        
        // Base score pad to ensure a perfectly strong, 0-failure, 0-hop peer can hit ~100
        // rssi=50 + hop=20 = 70. Add 30 base.
        score += 30f

        return score.toInt().coerceIn(0, 100)
    }
}
