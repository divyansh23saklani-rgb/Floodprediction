package com.example.flood.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.flood.MainActivity
import com.example.flood.R

object NotificationHelper {

    const val CHANNEL_ID_CRITICAL = "channel_flood_critical"
    const val CHANNEL_ID_WARNING = "channel_flood_warning"
    const val CHANNEL_ID_INCIDENTS = "channel_flood_incidents"

    const val NOTIF_ID_RISK_ALERT = 1001
    const val NOTIF_ID_INCIDENT_REPORT = 1002
    const val NOTIF_ID_SIMULATION = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Critical Flood & Disaster Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority sirens and emergency evacuation notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            val warningChannel = NotificationChannel(
                CHANNEL_ID_WARNING,
                "Weather & Flood Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Advisories and moderate rainfall warnings"
                enableVibration(true)
            }

            val incidentChannel = NotificationChannel(
                CHANNEL_ID_INCIDENTS,
                "Community Incident Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on landslides, roadblocks, and flood distress"
            }

            notificationManager.createNotificationChannels(listOf(criticalChannel, warningChannel, incidentChannel))
        }
    }

    fun sendRiskAlertNotification(
        context: Context,
        title: String,
        message: String,
        isCritical: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isCritical) CHANNEL_ID_CRITICAL else CHANNEL_ID_WARNING

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(if (isCritical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_RISK_ALERT, notification)
    }

    fun sendIncidentReportNotification(
        context: Context,
        typeLabel: String,
        severity: String,
        locationNote: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INCIDENTS)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("New Hazard Reported: $typeLabel")
            .setContentText("Severity: $severity • $locationNote")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Severity: $severity\nDetails: $locationNote"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_INCIDENT_REPORT + (System.currentTimeMillis() % 1000).toInt(), notification)
    }
}
