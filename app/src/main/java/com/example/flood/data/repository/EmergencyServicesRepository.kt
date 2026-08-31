package com.example.flood.data.repository

import com.example.flood.data.local.SeedData
import com.example.flood.data.model.EmergencyService

class EmergencyServicesRepository {

    fun getAllServices(): List<EmergencyService> {
        return SeedData.EMERGENCY_SERVICES
    }

    fun getServicesByType(type: String): List<EmergencyService> {
        return SeedData.EMERGENCY_SERVICES.filter { it.type.equals(type, ignoreCase = true) }
    }

    fun getNearestServices(userLat: Double, userLng: Double, limit: Int = 10): List<EmergencyService> {
        return SeedData.EMERGENCY_SERVICES
            .sortedBy { it.distanceTo(userLat, userLng) }
            .take(limit)
    }
}
