package com.meshlink.ble.discovery

/**
 * A simple 1D Kalman filter for smoothing noisy RSSI values and tracking signal variance.
 */
class KalmanFilter(
    private val processNoise: Double = 0.125, // Q: Expected variance in the process (how fast RSSI really changes)
    private val measurementNoise: Double = 4.0, // R: Expected variance in the measurements (how noisy the sensor is)
    private var estimatedError: Double = 1.0 // P: Initial error covariance
) {
    var estimate: Double? = null // X: Current estimate
        private set

    /**
     * Applies the Kalman filter to a new RSSI measurement.
     * @return The smoothed RSSI estimate.
     */
    fun filter(measurement: Double): Double {
        if (estimate == null) {
            estimate = measurement
            estimatedError = 1.0
            return measurement
        }

        // 1. Prediction Update
        // The estimated error increases due to process noise over time
        estimatedError += processNoise

        // 2. Measurement Update
        // Calculate Kalman Gain: how much we trust the new measurement vs our prediction
        val kalmanGain = estimatedError / (estimatedError + measurementNoise)
        
        // Update estimate
        estimate = estimate!! + kalmanGain * (measurement - estimate!!)
        
        // Update error covariance
        estimatedError = (1.0 - kalmanGain) * estimatedError

        return estimate!!
    }

    /**
     * Returns the current error covariance (variance of the estimate).
     * Smaller values mean higher confidence.
     */
    fun getVariance(): Double = estimatedError

    fun reset() {
        estimate = null
        estimatedError = 1.0
    }
}
