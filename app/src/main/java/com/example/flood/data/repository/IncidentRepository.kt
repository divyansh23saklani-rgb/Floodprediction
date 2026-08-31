package com.example.flood.data.repository

import com.example.flood.data.local.IncidentDao
import com.example.flood.data.local.SeedData
import com.example.flood.data.model.Incident
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class IncidentRepository(private val dao: IncidentDao) {

    val allIncidents: Flow<List<Incident>> = dao.getAllIncidents()

    fun getIncidentsByType(type: String): Flow<List<Incident>> = dao.getIncidentsByType(type)

    suspend fun addIncident(
        type: String,
        note: String,
        lat: Double,
        lng: Double,
        severity: String = "HIGH",
        createdAt: Long = System.currentTimeMillis()
    ): Incident {
        val incident = Incident(
            type = type,
            note = note,
            lat = lat,
            lng = lng,
            createdAt = createdAt,
            severity = severity,
            userReported = true,
            score = 0
        )
        val id = dao.insertIncident(incident)
        return incident.copy(id = id)
    }

    suspend fun insertRemoteIncident(incident: Incident): Boolean {
        val count = dao.checkExists(incident.createdAt, incident.lat, incident.lng, incident.type)
        if (count == 0) {
            dao.insertIncident(incident)
            return true
        }
        return false
    }

    suspend fun deleteIncident(id: Long) {
        dao.deleteById(id)
    }

    suspend fun resetToDefaults() {
        dao.clearAll()
        dao.insertAll(SeedData.INITIAL_INCIDENTS)
    }

    suspend fun seedIfEmpty() {
        if (dao.getIncidentCount() == 0) {
            dao.insertAll(SeedData.INITIAL_INCIDENTS)
        }
    }
}
