package com.meshlink.ai.engine

import com.meshlink.ai.data.LearningRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryPredictor @Inject constructor(
    private val learningRepository: LearningRepository
) {
    /**
     * Estimates the cost of a transaction based on learned hardware metrics.
     * @param bytes The size of the payload.
     * @return The estimated energy cost (arbitrary unit, scaled 0-100 for decision logic).
     */
    fun predictEnergyCost(bytes: Long): Float {
        // Retrieve learned baseline efficiency
        val bleEfficiency = learningRepository.getGlobalMetric("ble_bytes_per_unit", 5000f)

        return bytes.toFloat() / bleEfficiency
    }

    /**
     * Called after a transfer to refine the model.
     * Assuming the OS told us battery dropped by X micro-amp hours.
     */
    fun learnTransferCost(bytes: Long, energyUsed: Float) {
        if (energyUsed <= 0) return
        val currentEfficiency = bytes.toFloat() / energyUsed
        val key = "ble_bytes_per_unit"
        
        val baseline = learningRepository.getGlobalMetric(key, 5000f)
        
        // EWMA update
        val newBaseline = (baseline * 0.9f) + (currentEfficiency * 0.1f)
        learningRepository.setGlobalMetric(key, newBaseline)
    }
}
