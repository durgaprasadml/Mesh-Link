package com.meshlink.enterprise.deployment

import javax.inject.Inject
import javax.inject.Singleton

data class OperationsReport(
    val timestamp: Long,
    val fleetStatusCsv: String,
    val integrityVerified: Boolean,
    val backupVerified: Boolean
)

@Singleton
class OperationsManager @Inject constructor(
    private val fleetManager: FleetManagementManager
) {

    fun generateWeeklyOperationsReport(nodeId: String, batteryPercent: Int, storageMb: Long, compliance: Float, dbHealthy: Boolean, backupHealthy: Boolean): OperationsReport {
        val status = fleetManager.generateLocalFleetStatus(nodeId, batteryPercent, storageMb, compliance)
        
        return OperationsReport(
            timestamp = System.currentTimeMillis(),
            fleetStatusCsv = fleetManager.exportToCsv(status),
            integrityVerified = dbHealthy,
            backupVerified = backupHealthy
        )
    }
}
