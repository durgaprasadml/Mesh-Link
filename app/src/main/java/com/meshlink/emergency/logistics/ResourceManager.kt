package com.meshlink.emergency.logistics

import com.meshlink.common.logger.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ResourceCategory {
    WATER, FOOD, MEDICAL, FUEL, VEHICLE, PERSONNEL, TOOLS
}

data class Resource(
    val id: String,
    val category: ResourceCategory,
    val name: String,
    var quantity: Int,
    val location: String,
    val ownerId: String
)

@Singleton
class ResourceManager @Inject constructor() {
    
    companion object {
        private const val TAG = "ResourceManager"
    }

    private val _resources = MutableStateFlow<Map<String, Resource>>(emptyMap())
    val resources: StateFlow<Map<String, Resource>> = _resources.asStateFlow()

    fun declareResource(category: ResourceCategory, name: String, quantity: Int, location: String, ownerId: String) {
        val id = java.util.UUID.randomUUID().toString()
        val resource = Resource(id, category, name, quantity, location, ownerId)
        
        val current = _resources.value.toMutableMap()
        current[id] = resource
        _resources.value = current
        
        MeshLogger.d(TAG, "Declared new resource: $quantity x $name at $location")
    }

    fun syncResource(resource: Resource) {
        val current = _resources.value.toMutableMap()
        current[resource.id] = resource
        _resources.value = current
        MeshLogger.d(TAG, "Synced remote resource state: ${resource.name} (${resource.quantity})")
    }

    fun consumeResource(resourceId: String, amount: Int) {
        val current = _resources.value.toMutableMap()
        val resource = current[resourceId]
        if (resource != null) {
            resource.quantity = maxOf(0, resource.quantity - amount)
            current[resourceId] = resource
            _resources.value = current
            MeshLogger.d(TAG, "Consumed $amount of ${resource.name}. Remaining: ${resource.quantity}")
        }
    }
}
