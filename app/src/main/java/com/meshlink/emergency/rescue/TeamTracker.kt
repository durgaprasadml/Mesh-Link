package com.meshlink.emergency.rescue

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class TeamRole {
    COMMANDER, MEDIC, FIRE, POLICE, VOLUNTEER, SCOUT
}

enum class TeamMemberStatus {
    SAFE, INJURED, TRAPPED, NEED_HELP, UNKNOWN, OFFLINE
}

data class TeamMember(
    val id: String,
    val name: String,
    val role: TeamRole,
    var status: TeamMemberStatus,
    var lastSeenMs: Long,
    var batteryPct: Int
)

@Singleton
class TeamTracker @Inject constructor() {

    companion object {
        private const val TAG = "TeamTracker"
        private const val OFFLINE_THRESHOLD_MS = 300_000L // 5 minutes without check-in = offline
    }

    private val _teamMembers = MutableStateFlow<Map<String, TeamMember>>(emptyMap())
    val teamMembers: StateFlow<Map<String, TeamMember>> = _teamMembers.asStateFlow()

    fun processCheckIn(id: String, name: String, role: TeamRole, status: TeamMemberStatus, batteryPct: Int) {
        val currentMap = _teamMembers.value.toMutableMap()
        
        currentMap[id] = TeamMember(
            id = id,
            name = name,
            role = role,
            status = status,
            lastSeenMs = System.currentTimeMillis(),
            batteryPct = batteryPct
        )
        
        _teamMembers.value = currentMap
        MeshLogger.d(TAG, "Team Check-in: $name ($role) is $status [Batt: $batteryPct%]")
    }

    fun evaluateOfflineStatus() {
        val now = System.currentTimeMillis()
        var changed = false
        val currentMap = _teamMembers.value.toMutableMap()
        
        for ((id, member) in currentMap) {
            if (member.status != TeamMemberStatus.OFFLINE && now - member.lastSeenMs > OFFLINE_THRESHOLD_MS) {
                currentMap[id] = member.copy(status = TeamMemberStatus.OFFLINE)
                changed = true
                MeshLogger.w(TAG, "Team member ${member.name} has gone OFFLINE.")
            }
        }
        
        if (changed) {
            _teamMembers.value = currentMap
        }
    }
}
