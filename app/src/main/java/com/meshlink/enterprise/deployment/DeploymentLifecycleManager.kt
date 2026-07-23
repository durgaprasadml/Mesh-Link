package com.meshlink.enterprise.deployment

import javax.inject.Inject
import javax.inject.Singleton

enum class DeploymentStage {
    PILOT, PRODUCTION, LTS, EMERGENCY_PATCH, ROLLBACK
}

@Singleton
class DeploymentLifecycleManager @Inject constructor() {

    fun verifyMigrationSafety(currentDbVersion: Int, targetDbVersion: Int, stage: DeploymentStage): Boolean {
        // Enforce strict migration rules based on the deployment tier
        
        if (stage == DeploymentStage.LTS) {
            // Long Term Support builds should never attempt experimental migrations
            return currentDbVersion == targetDbVersion
        }
        
        if (stage == DeploymentStage.ROLLBACK) {
            // We cannot safely migrate a database backwards in Room natively without data loss
            // Rely on the RecoveryManager's Backup swap instead.
            return false
        }
        
        // Standard forward migration allowed for Pilot/Production
        return targetDbVersion > currentDbVersion
    }
}
