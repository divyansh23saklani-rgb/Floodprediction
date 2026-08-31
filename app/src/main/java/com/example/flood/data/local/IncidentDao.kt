package com.example.flood.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flood.data.model.Incident
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<Incident>>

    @Query("SELECT * FROM incidents WHERE type = :type ORDER BY createdAt DESC")
    fun getIncidentsByType(type: String): Flow<List<Incident>>

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int

    @Query("SELECT COUNT(*) FROM incidents WHERE createdAt = :createdAt")
    suspend fun checkExists(createdAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: Incident): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<Incident>)

    @Update
    suspend fun updateIncident(incident: Incident)

    @Delete
    suspend fun deleteIncident(incident: Incident)

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: Long): Incident?

    @Query("SELECT * FROM incidents WHERE createdAt = :createdAt LIMIT 1")
    suspend fun getIncidentByCreatedAt(createdAt: Long): Incident?

    @Query("UPDATE incidents SET upvotes = :upvotes, downvotes = :downvotes, score = :score, userVote = :userVote WHERE id = :id")
    suspend fun updateVotes(id: Long, upvotes: Int, downvotes: Int, score: Int, userVote: Int)

    @Query("UPDATE incidents SET upvotes = :upvotes, downvotes = :downvotes, score = :score WHERE createdAt = :createdAt")
    suspend fun updateVotesByCreatedAt(createdAt: Long, upvotes: Int, downvotes: Int, score: Int)

    @Query("UPDATE incidents SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE incidents SET status = :status WHERE createdAt = :createdAt")
    suspend fun updateStatusByCreatedAt(createdAt: Long, status: String)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incidents")
    suspend fun clearAll()
}
