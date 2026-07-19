package com.meshlink.enterprise.governance

import javax.inject.Inject
import javax.inject.Singleton

enum class RiskLevel {
    INFORMATIONAL, LOW, MEDIUM, HIGH, CRITICAL
}

data class RiskItem(
    val id: String,
    val description: String,
    val level: RiskLevel
)

@Singleton
class RiskManager @Inject constructor() {

    fun assessOperationalRisk(batteryPercent: Int, backupAgeMs: Long, hasSplitBrain: Boolean): List<RiskItem> {
        val risks = mutableListOf<RiskItem>()
        
        if (batteryPercent < 15) {
            risks.add(RiskItem("BAT-01", "Battery critically low, node may drop off mesh", RiskLevel.HIGH))
        }
        
        if (backupAgeMs > 86400000L) { // > 24 hours
            risks.add(RiskItem("BKP-01", "Local SQLCipher backup is older than 24 hours", RiskLevel.MEDIUM))
        }
        
        if (hasSplitBrain) {
            risks.add(RiskItem("NET-01", "Mesh network partition detected, partial fleet visibility", RiskLevel.CRITICAL))
        }
        
        if (risks.isEmpty()) {
            risks.add(RiskItem("SYS-01", "System operating normally", RiskLevel.INFORMATIONAL))
        }
        
        return risks
    }
}
