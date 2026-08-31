package com.example.flood.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.flood.data.local.AppDatabase
import com.example.flood.data.remote.CloudIncidentSyncManager
import com.example.flood.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DisasterSyncService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var streamJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        try {
            startForeground(
                NotificationHelper.NOTIF_ID_SERVICE,
                NotificationHelper.getSyncServiceNotification(this)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
        }
        startLiveStreamListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (streamJob == null || streamJob?.isActive != true) {
            startLiveStreamListener()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLiveStreamListener() {
        streamJob?.cancel()
        streamJob = serviceScope.launch {
            val myDeviceId = CloudIncidentSyncManager.getDeviceId(this@DisasterSyncService)
            val db = AppDatabase.getDatabase(this@DisasterSyncService)

            while (isActive) {
                var connection: HttpURLConnection? = null
                try {
                    // Connect to continuous Server-Sent Events / streaming json endpoint for sub-second real-time delivery
                    val streamUrl = URL("https://ntfy.sh/jaldrishti_disaster_sync_v1/json")
                    connection = (streamUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 0 // Infinite read timeout for continuous stream
                        doInput = true
                    }

                    if (connection.responseCode in 200..299) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

                        while (isActive) {
                            val line = reader.readLine() ?: break
                            val trimmed = line.trim()
                            if (trimmed.isEmpty() || !trimmed.startsWith("{")) continue

                            try {
                                val eventJson = JSONObject(trimmed)
                                if (eventJson.optString("event") == "message") {
                                    val messageBody = eventJson.optString("message")
                                    val event = CloudIncidentSyncManager.parseMessage(messageBody, myDeviceId)

                                    when (event) {
                                        is CloudIncidentSyncManager.RemoteEvent.NewIncident -> {
                                            val inc = event.incident
                                            val exists = db.incidentDao().checkExists(inc.createdAt, inc.lat, inc.lng, inc.type)
                                            if (exists == 0) {
                                                db.incidentDao().insertIncident(inc)
                                                NotificationHelper.sendIncidentReportNotification(
                                                    context = this@DisasterSyncService,
                                                    typeLabel = inc.type.uppercase(),
                                                    severity = inc.severity,
                                                    locationNote = inc.note.ifBlank { "Location: (${inc.lat}, ${inc.lng})" }
                                                )
                                            }
                                        }
                                        is CloudIncidentSyncManager.RemoteEvent.NewComment -> {
                                            val comment = event.comment
                                            val exists = db.commentDao().checkExists(
                                                incidentId = comment.incidentId,
                                                incidentCreatedAt = comment.incidentCreatedAt,
                                                createdAt = comment.createdAt,
                                                text = comment.text
                                            )
                                            if (exists == 0) {
                                                db.commentDao().insertComment(comment)
                                                NotificationHelper.sendCommentNotification(
                                                    context = this@DisasterSyncService,
                                                    author = comment.authorName,
                                                    commentText = comment.text,
                                                    hazardType = "Community Hazard Report"
                                                )
                                            }
                                        }
                                        is CloudIncidentSyncManager.RemoteEvent.VoteUpdate -> {
                                            db.incidentDao().updateVotesByCreatedAt(
                                                createdAt = event.incidentCreatedAt,
                                                upvotes = event.upvotes,
                                                downvotes = event.downvotes,
                                                score = event.score
                                            )
                                        }
                                        is CloudIncidentSyncManager.RemoteEvent.StatusUpdate -> {
                                            db.incidentDao().updateStatusByCreatedAt(
                                                createdAt = event.incidentCreatedAt,
                                                status = event.status
                                            )
                                        }
                                        null -> { /* non-actionable or self message */ }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error handling incoming stream line: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Stream connection closed or error: ${e.message}, reconnecting...")
                } finally {
                    try {
                        connection?.disconnect()
                    } catch (e: Exception) { /* ignore */ }
                }

                // If connection drops, wait briefly and auto-reconnect
                delay(3000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "DisasterSyncService"

        fun start(context: Context) {
            val intent = Intent(context, DisasterSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }
    }
}
