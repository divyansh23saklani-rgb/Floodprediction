package com.example.flood.data.repository

import com.example.flood.data.model.RiskLevel
import com.example.flood.data.model.WeatherRisk
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WeatherRepository {

    fun calculatePrecipitationNext6h(lat: Double, lng: Double): Double {
        // Faithful to original src/api/weather.ts
        val base = max(0.0, 12.0 - abs(30.73 - lat) * 40.0 - abs(78.44 - lng) * 40.0)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val diurnal = when {
            hour in 15..22 -> 4.0
            hour in 6..9 -> 2.0
            else -> 0.0
        }
        val noise = Math.random() * 1.5
        val p = max(0.0, base + diurnal + noise)
        return min(20.0, p)
    }

    fun evaluateRisk(
        precipMmPerHour: Double,
        yellowThreshold: Double = 5.0,
        redThreshold: Double = 10.0
    ): WeatherRisk {
        val riskLevel = when {
            precipMmPerHour >= redThreshold -> RiskLevel.RED
            precipMmPerHour >= yellowThreshold -> RiskLevel.YELLOW
            else -> RiskLevel.GREEN
        }

        val advisory = when (riskLevel) {
            RiskLevel.RED -> "Severe Flood & Flash Surge Warning! Heavy continuous precipitation predicted in catchment. Move to elevated safe shelters."
            RiskLevel.YELLOW -> "Flood Watch Alert: Moderate-to-heavy rainfall. River discharge increasing. Exercise caution around low-lying riverbeds."
            RiskLevel.GREEN -> "Normal Hydrological Conditions: Stable river runoff. No active inundation warning for current sector."
        }

        val riverLevel = when (riskLevel) {
            RiskLevel.RED -> "Danger Mark Exceeded (+2.8m)"
            RiskLevel.YELLOW -> "Warning Level Approaching (+1.1m)"
            RiskLevel.GREEN -> "Normal Seasonal Discharge"
        }

        return WeatherRisk(
            precipitationMm = Math.round(precipMmPerHour * 10.0) / 10.0,
            riskLevel = riskLevel,
            advisory = advisory,
            riverWaterLevel = riverLevel,
            catchmentRainfall = Math.round(precipMmPerHour * 1.6 * 10.0) / 10.0,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
