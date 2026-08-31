package com.example.flood.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flood.data.model.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM incident_comments WHERE (:incidentId != 0 AND incidentId = :incidentId) OR (:incidentCreatedAt != 0 AND incidentCreatedAt = :incidentCreatedAt) ORDER BY createdAt ASC")
    fun getCommentsForIncident(incidentId: Long, incidentCreatedAt: Long): Flow<List<Comment>>

    @Query("SELECT * FROM incident_comments ORDER BY createdAt ASC")
    fun getAllComments(): Flow<List<Comment>>

    @Query("SELECT COUNT(*) FROM incident_comments WHERE createdAt = :createdAt AND text = :text")
    suspend fun checkExists(createdAt: Long, text: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment): Long

    @Query("DELETE FROM incident_comments WHERE id = :id")
    suspend fun deleteComment(id: Long)

    @Query("DELETE FROM incident_comments WHERE incidentId = :incidentId")
    suspend fun deleteCommentsForIncident(incidentId: Long)
}
