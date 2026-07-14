package com.meshlink.ble.discovery

/**
 * Exponential Moving Average (EMA) filter for smoothing noisy RSSI values.
 */
class RssiFilter(private val alpha: Float = 0.25f) {
    private var smoothedRssi: Float? = null

    /**
     * Applies the filter to a new raw RSSI reading.
     * @return The smoothed RSSI value.
     */
    fun filter(rawRssi: Int): Int {
        val currentRssi = rawRssi.toFloat()
        val previous = smoothedRssi
        
        val newSmoothed = if (previous == null) {
            currentRssi
        } else {
            alpha * currentRssi + (1 - alpha) * previous
        }
        
        smoothedRssi = newSmoothed
        return newSmoothed.toInt()
    }
    
    fun reset() {
        smoothedRssi = null
    }
}
