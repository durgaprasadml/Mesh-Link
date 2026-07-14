package com.meshlink.emergency.incident

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class IncidentType {
    EARTHQUAKE, FLOOD, CYCLONE, WILDFIRE, LANDSLIDE, WAR_ZONE, INDUSTRIAL_ACCIDENT, MASS_GATHERING, MEDICAL
}

enum class IncidentStatus {
    ACTIVE, CONTAINED, RESOLVED
}

data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val type: IncidentType,
    val severity: Int, // 1 to 5
    val locationDescription: String,
    val commanderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    var status: IncidentStatus = IncidentStatus.ACTIVE
)

@Singleton
class IncidentManager @Inject constructor() {

    companion object {
        private const val TAG = "IncidentManager"
    }

    private val _activeIncidents = MutableStateFlow<List<Incident>>(emptyList())
    val activeIncidents: StateFlow<List<Incident>> = _activeIncidents.asStateFlow()
    
    fun declareIncident(type: IncidentType, severity: Int, location: String, commanderId: String): Incident {
        val incident = Incident(
            type = type,
            severity = severity.coerceIn(1, 5),
            locationDescription = location,
            commanderId = commanderId
        )
        
        val currentList = _activeIncidents.value.toMutableList()
        currentList.add(incident)
        _activeIncidents.value = currentList
        
        MeshLogger.w(TAG, "New Incident Declared: ${incident.type} (Severity ${incident.severity}) at ${incident.locationDescription}")
        
        return incident
    }
    
    fun syncIncomingIncident(incident: Incident) {
        val currentList = _activeIncidents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == incident.id }
        if (index == -1) {
            currentList.add(incident)
            _activeIncidents.value = currentList
            MeshLogger.d(TAG, "Synced remote incident: ${incident.id}")
        } else {
            // Update existing if newer status
            if (currentList[index].status != incident.status) {
                currentList[index] = incident
                _activeIncidents.value = currentList
                MeshLogger.d(TAG, "Updated remote incident: ${incident.id}")
            }
        }
    }
}
