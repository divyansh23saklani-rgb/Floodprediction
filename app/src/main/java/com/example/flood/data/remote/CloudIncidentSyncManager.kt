package com.example.flood.data.remote

import android.content.Context
import android.util.Log
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
     * Broadcasts an incident to all connected devices on the shared disaster alert network.
     */
    suspend fun publishIncident(context: Context, incident: Incident): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val url = URL(SYNC_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Title", "🚨 Community Disaster: ${incident.type.uppercase()}")
                setRequestProperty("Priority", if (incident.severity.equals("HIGH", ignoreCase = true)) "5" else "4")
                setRequestProperty("Tags", "warning,rotating_light")
            }

            val payload = JSONObject().apply {
                put("type", incident.type)
                put("note", incident.note)
                put("lat", incident.lat)
                put("lng", incident.lng)
                put("severity", incident.severity)
                put("createdAt", incident.createdAt)
                put("senderDeviceId", deviceId)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Published incident to cloud relay. Response code: $responseCode")
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish incident to cloud relay: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches recent community incidents from the shared cloud relay.
     * Returns a list of new Incidents originating from OTHER devices.
     */
    suspend fun fetchRemoteIncidents(context: Context): List<Incident> = withContext(Dispatchers.IO) {
        val remoteList = mutableListOf<Incident>()
        try {
            val myDeviceId = getDeviceId(context)
            // Query recent 24-48 hours of published incidents
            val queryUrl = URL("$SYNC_URL/json?poll=1&since=48h")
            val connection = (queryUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.isEmpty() || !trimmed.startsWith("{")) continue
                        try {
                            val eventJson = JSONObject(trimmed)
                            if (eventJson.optString("event") == "message") {
                                val messageBody = eventJson.optString("message")
                                if (messageBody.isNotBlank() && messageBody.startsWith("{")) {
                                    val incJson = JSONObject(messageBody)
                                    val sender = incJson.optString("senderDeviceId")
                                    // Skip messages sent by this device itself
                                    if (sender == myDeviceId) {
                                        continue
                                    }

                                    val type = incJson.optString("type", "flood")
                                    val note = incJson.optString("note", "")
                                    val lat = incJson.optDouble("lat", 0.0)
                                    val lng = incJson.optDouble("lng", 0.0)
                                    val severity = incJson.optString("severity", "HIGH")
                                    val createdAt = incJson.optLong("createdAt", eventJson.optLong("time") * 1000L)

                                    if (lat != 0.0 && lng != 0.0) {
                                        remoteList.add(
                                            Incident(
                                                type = type,
                                                note = note,
                                                lat = lat,
                                                lng = lng,
                                                createdAt = createdAt,
                                                severity = severity,
                                                userReported = true,
                                                score = 0
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (pe: Exception) {
                            // Non-json or different event line, skip
                        }
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync remote incidents: ${e.message}")
        }
        remoteList
    }
}
