package com.meshlink.ble.discovery

import kotlin.math.pow

enum class Confidence { HIGH, MEDIUM, LOW }

data class DistanceEstimate(
    val distanceMeters: Double,
    val confidence: Confidence
)

object DistanceEstimator {
    // Calibration constants. -59 dBm is typical for 1 meter distance in BLE.
    private const val TX_POWER = -59.0
    // Path loss exponent. 2.0 is typical for free space. Can be between 2.0 and 4.0 depending on environment.
    private const val PATH_LOSS_EXPONENT = 2.0

    /**
     * Estimates the distance in meters and assigns a confidence score based on RSSI variance.
     * 
     * @param rssi The smoothed RSSI value from the Kalman filter
     * @param errorCovariance The variance/error from the Kalman filter
     * @return DistanceEstimate object containing the distance and confidence level
     */
    fun estimateDistance(rssi: Double, errorCovariance: Double): DistanceEstimate {
        // Determine confidence based on variance (errorCovariance).
        // Lower variance = more stable signal = higher confidence.
        val confidence = when {
            errorCovariance < 5.0 -> Confidence.HIGH
            errorCovariance < 15.0 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        
        // Simple path loss formula
        val distance = 10.0.pow((TX_POWER - rssi) / (10 * PATH_LOSS_EXPONENT))
        
        return DistanceEstimate(distance, confidence)
    }
}
