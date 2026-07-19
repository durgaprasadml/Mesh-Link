package com.meshlink.enterprise.deployment

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

data class FleetStatus(
    val nodeId: String,
    val appVersion: String,
    val osVersion: String,
    val batteryPercent: Int,
    val storageAvailableMb: Long,
    val complianceScore: Float
)

@Singleton
class FleetManagementManager @Inject constructor() {

    fun generateLocalFleetStatus(nodeId: String, batteryPercent: Int, storageMb: Long, compliance: Float): FleetStatus {
        return FleetStatus(
            nodeId = nodeId,
            appVersion = "1.8.0-enterprise", // This would normally be pulled from BuildConfig
            osVersion = Build.VERSION.RELEASE,
            batteryPercent = batteryPercent,
            storageAvailableMb = storageMb,
            complianceScore = compliance
        )
    }

    fun exportToCsv(status: FleetStatus): String {
        return "NodeID,AppVersion,OS,Battery,StorageMB,Compliance\n" +
               "${status.nodeId},${status.appVersion},${status.osVersion},${status.batteryPercent},${status.storageAvailableMb},${status.complianceScore}"
    }
}
