package com.example.flood.data.model

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ServiceType(val label: String, val emoji: String, val colorHex: String) {
    HOSPITAL("Hospital / Medical", "🏥", "#16A34A"),
    SHELTER("Relief Shelter", "🏕️", "#D97706"),
    POLICE("Police / Emergency", "👮", "#0284C7")
}

@Serializable
data class EmergencyService(
    val id: String,
    val name: String,
    val type: String, // hospital, shelter, police
    val lat: Double,
    val lng: Double,
    val address: String,
    val phone: String = "+91-1374-222107"
) {
    val serviceType: ServiceType
        get() = when (type.lowercase()) {
            "hospital" -> ServiceType.HOSPITAL
            "shelter", "relief" -> ServiceType.SHELTER
            "police" -> ServiceType.POLICE
            else -> ServiceType.HOSPITAL
        }

    fun distanceTo(userLat: Double, userLng: Double): Double {
        val toRad = { d: Double -> d * Math.PI / 180.0 }
        val radius = 6371.0 // Earth radius in km
        val dLat = toRad(lat - userLat)
        val dLng = toRad(lng - userLng)
        val lat1 = toRad(userLat)
        val lat2 = toRad(lat)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * asin(sqrt(a))
        return radius * c
    }
}
