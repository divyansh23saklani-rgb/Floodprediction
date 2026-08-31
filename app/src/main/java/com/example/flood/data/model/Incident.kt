package com.example.flood.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class IncidentType(val label: String, val emoji: String, val colorHex: String) {
    FLOOD("Flood", "🌊", "#DC2626"),
    LANDSLIDE("Landslide", "⛰️", "#EA580C"),
    TREE("Tree Fall", "🌳", "#16A34A"),
    ROAD("Road Block", "🚧", "#D97706"),
    DISTRESS("Distress / SOS", "🆘", "#9333EA"),
    YELLOW("Waterlogging", "⚠️", "#EAB308");

    companion object {
        fun fromString(value: String): IncidentType {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: FLOOD
        }
    }
}

@Serializable
@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // flood, landslide, tree, road, distress, yellow
    val note: String = "",
    val lat: Double,
    val lng: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val severity: String = "HIGH", // HIGH, MEDIUM, LOW
    val userReported: Boolean = true,
    val score: Int = 0,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val status: String = "OPEN", // "OPEN", "RESOLVED"
    val userVote: Int = 0 // 1: upvoted, -1: downvoted, 0: none
) {
    val incidentType: IncidentType
        get() = IncidentType.fromString(type)

    val isOpen: Boolean
        get() = status.equals("OPEN", ignoreCase = true)
}
