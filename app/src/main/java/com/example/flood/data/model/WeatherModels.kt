package com.example.flood.data.model

import kotlinx.serialization.Serializable

enum class RiskLevel(val label: String, val colorCode: String, val colorHex: String) {
    GREEN("Low Risk (Normal)", "green", "#16A34A"),
    YELLOW("Moderate Risk (Watch)", "orange", "#EA580C"),
    RED("Severe Flood Risk (Warning)", "red", "#DC2626")
}

@Serializable
data class WeatherRisk(
    val precipitationMm: Double,
    val riskLevel: RiskLevel,
    val advisory: String,
    val riverWaterLevel: String = "Normal Flow",
    val catchmentRainfall: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class DisasterSimulation(
    val id: String,
    val title: String,
    val year: String,
    val description: String,
    val rainIntensity: Int,
    val riskColor: String,
    val simulatedIncidents: List<Incident>
)
