package com.example.flood.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "incident_comments")
data class Comment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val incidentId: Long,
    val incidentCreatedAt: Long = 0L,
    val authorName: String = "Community Member",
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val senderDeviceId: String = ""
)
