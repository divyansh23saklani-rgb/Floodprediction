package com.example.flood.data.remote

import android.content.Context
import android.util.Log
import com.example.flood.data.model.Comment
import com.example.flood.data.model.Incident
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object CloudIncidentSyncManager {

    private const val TAG = "CloudSyncManager"
    private const val PREFS_NAME = "disaster_sync_prefs"
    private const val KEY_DEVICE_ID = "device_unique_id"
    private const val SYNC_TOPIC = "jaldrishti_disaster_sync_v1"
    private const val SYNC_URL = "https://ntfy.sh/$SYNC_TOPIC"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    /**
     * Broadcasts a newly reported incident to all connected devices.
     */
    suspend fun publishIncident(context: Context, incident: Incident): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val payload = JSONObject().apply {
                put("action", "incident")
                put("type", incident.type)
                put("note", incident.note)
                put("lat", incident.lat)
                put("lng", incident.lng)
                put("severity", incident.severity)
                put("createdAt", incident.createdAt)
                put("senderDeviceId", deviceId)
            }
            sendPayload(
                payload = payload,
                title = "🚨 Community Hazard: ${incident.type.uppercase()}",
                priority = if (incident.severity.equals("HIGH", ignoreCase = true)) "5" else "4",
                tags = "warning,rotating_light"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish incident: ${e.message}", e)
            false
        }
    }

    /**
     * Broadcasts a new comment on an incident.
     */
    suspend fun publishComment(context: Context, comment: Comment): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val payload = JSONObject().apply {
                put("action", "comment")
                put("incidentId", comment.incidentId)
                put("incidentCreatedAt", comment.incidentCreatedAt)
                put("authorName", comment.authorName)
                put("text", comment.text)
                put("createdAt", comment.createdAt)
                put("senderDeviceId", deviceId)
            }
            sendPayload(
                payload = payload,
                title = "💬 Update on Hazard Report",
                priority = "3",
                tags = "speech_balloon"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish comment: ${e.message}", e)
            false
        }
    }

    /**
     * Broadcasts updated votes for an incident.
     */
    suspend fun publishVote(
        context: Context,
        incidentCreatedAt: Long,
        upvotes: Int,
        downvotes: Int,
        score: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val payload = JSONObject().apply {
                put("action", "vote")
                put("incidentCreatedAt", incidentCreatedAt)
                put("upvotes", upvotes)
                put("downvotes", downvotes)
                put("score", score)
                put("senderDeviceId", deviceId)
            }
            sendPayload(
                payload = payload,
                title = "👍 Incident Verified by Community",
                priority = "2",
                tags = "thumbsup"
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Broadcasts status change (e.g. OPEN or RESOLVED).
     */
    suspend fun publishStatus(
        context: Context,
        incidentCreatedAt: Long,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val payload = JSONObject().apply {
                put("action", "status")
                put("incidentCreatedAt", incidentCreatedAt)
                put("status", status)
                put("senderDeviceId", deviceId)
            }
            sendPayload(
                payload = payload,
                title = "✅ Hazard Status: $status",
                priority = "3",
                tags = "check"
            )
        } catch (e: Exception) {
            false
        }
    }

    private fun sendPayload(payload: JSONObject, title: String, priority: String, tags: String): Boolean {
        // 1. Try standard ntfy.sh JSON publishing endpoint
        try {
            val url = URL("https://ntfy.sh")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val ntfyBody = JSONObject().apply {
                put("topic", SYNC_TOPIC)
                put("title", title)
                put("message", payload.toString())
                put("priority", priority.toIntOrNull() ?: 4)
                if (tags.isNotBlank()) {
                    val tagsArray = org.json.JSONArray()
                    tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tagsArray.put(it) }
                    put("tags", tagsArray)
                }
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(ntfyBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()
            if (responseCode in 200..299) {
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "ntfy root POST failed: ${e.message}, trying topic endpoint...")
        }

        // 2. Fallback: Publish directly to topic endpoint
        try {
            val url = URL(SYNC_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                setRequestProperty("Title", title)
                setRequestProperty("Priority", priority)
                if (tags.isNotBlank()) {
                    setRequestProperty("Tags", tags)
                }
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()
            return responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "sendPayload fallback error: ${e.message}", e)
            return false
        }
    }

    sealed class RemoteEvent {
        data class NewIncident(val incident: Incident) : RemoteEvent()
        data class NewComment(val comment: Comment) : RemoteEvent()
        data class VoteUpdate(val incidentCreatedAt: Long, val upvotes: Int, val downvotes: Int, val score: Int) : RemoteEvent()
        data class StatusUpdate(val incidentCreatedAt: Long, val status: String) : RemoteEvent()
    }

    fun parseMessage(messageBody: String, myDeviceId: String): RemoteEvent? {
        if (messageBody.isBlank()) return null
        return try {
            val trimmed = messageBody.trim()
            if (!trimmed.startsWith("{")) return null
            var json = JSONObject(trimmed)

            // Unwrap nested wrappers if present
            if (json.has("message") && !json.has("action") && !json.has("lat")) {
                val inner = json.opt("message")
                if (inner is JSONObject) {
                    json = inner
                } else if (inner is String && inner.trim().startsWith("{")) {
                    json = JSONObject(inner.trim())
                }
            }
            if (json.has("payload") && !json.has("action") && !json.has("lat")) {
                val inner = json.opt("payload")
                if (inner is JSONObject) {
                    json = inner
                } else if (inner is String && inner.trim().startsWith("{")) {
                    json = JSONObject(inner.trim())
                }
            }

            val sender = json.optString("senderDeviceId", "")
            if (sender.isNotBlank() && sender == myDeviceId) {
                return null // Ignore self messages
            }

            val rawAction = json.optString("action", "")
            val effectiveAction = when {
                rawAction.isNotBlank() -> rawAction
                json.has("lat") && json.has("lng") -> "incident"
                json.has("authorName") && json.has("text") -> "comment"
                json.has("upvotes") || json.has("score") -> "vote"
                json.has("status") && json.has("incidentCreatedAt") -> "status"
                else -> ""
            }

            when (effectiveAction) {
                "incident" -> {
                    val type = json.optString("type", "accident")
                    val note = json.optString("note", "")
                    val lat = json.optDouble("lat", 0.0)
                    val lng = json.optDouble("lng", 0.0)
                    val severity = json.optString("severity", "HIGH")
                    val createdAt = json.optLong("createdAt", System.currentTimeMillis())

                    if (lat != 0.0 && lng != 0.0) {
                        RemoteEvent.NewIncident(
                            Incident(
                                type = type,
                                note = note,
                                lat = lat,
                                lng = lng,
                                createdAt = createdAt,
                                severity = severity,
                                userReported = true,
                                score = 0,
                                upvotes = 0,
                                downvotes = 0,
                                status = "OPEN",
                                userVote = 0
                            )
                        )
                    } else null
                }
                "comment" -> {
                    val incidentId = json.optLong("incidentId", 0L)
                    val incidentCreatedAt = json.optLong("incidentCreatedAt", 0L)
                    val authorName = json.optString("authorName", "Community Member")
                    val text = json.optString("text", "")
                    val createdAt = json.optLong("createdAt", System.currentTimeMillis())
                    if (text.isNotBlank()) {
                        RemoteEvent.NewComment(
                            Comment(
                                incidentId = incidentId,
                                incidentCreatedAt = incidentCreatedAt,
                                authorName = authorName,
                                text = text,
                                createdAt = createdAt,
                                senderDeviceId = sender
                            )
                        )
                    } else null
                }
                "vote" -> {
                    val incidentCreatedAt = json.optLong("incidentCreatedAt", 0L)
                    val upvotes = json.optInt("upvotes", 0)
                    val downvotes = json.optInt("downvotes", 0)
                    val score = json.optInt("score", 0)
                    if (incidentCreatedAt != 0L) {
                        RemoteEvent.VoteUpdate(incidentCreatedAt, upvotes, downvotes, score)
                    } else null
                }
                "status" -> {
                    val incidentCreatedAt = json.optLong("incidentCreatedAt", 0L)
                    val status = json.optString("status", "OPEN")
                    if (incidentCreatedAt != 0L) {
                        RemoteEvent.StatusUpdate(incidentCreatedAt, status)
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseMessage error: ${e.message}", e)
            null
        }
    }

    /**
     * Polls recent events from the shared cloud network.
     */
    suspend fun fetchRecentEvents(context: Context): List<RemoteEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<RemoteEvent>()
        try {
            val myDeviceId = getDeviceId(context)
            val queryUrl = URL("$SYNC_URL/json?poll=1&since=all")
            val connection = (queryUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.isEmpty() || !trimmed.startsWith("{")) continue
                        try {
                            val eventJson = JSONObject(trimmed)
                            val messageBody = if (eventJson.has("message")) {
                                eventJson.optString("message")
                            } else {
                                trimmed
                            }
                            val event = parseMessage(messageBody, myDeviceId)
                                ?: parseMessage(trimmed, myDeviceId)
                            if (event != null) {
                                events.add(event)
                            }
                        } catch (e: Exception) {
                            // skip individual malformed line
                        }
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Poll failed: ${e.message}")
        }
        events
    }
}
