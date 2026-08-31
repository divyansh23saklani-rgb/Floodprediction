package com.example.flood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flood.util.NotificationHelper

@Composable
fun SettingsDialog(
    currentRadiusKm: Double,
    currentYellowThreshold: Double,
    currentRedThreshold: Double,
    onSave: (radiusKm: Double, yellowThreshold: Double, redThreshold: Double) -> Unit,
    onResetData: () -> Unit,
    onDismiss: () -> Unit
) {
    var radius by remember { mutableFloatStateOf(currentRadiusKm.toFloat()) }
    var yellow by remember { mutableFloatStateOf(currentYellowThreshold.toFloat()) }
    var red by remember { mutableFloatStateOf(currentRedThreshold.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = "Alert Thresholds & Range", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Alert Radius Slider
                Text(
                    text = "Monitoring Alert Radius: ${radius.toInt()} km",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0284C7),
                        activeTrackColor = Color(0xFF0284C7)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Yellow threshold
                Text(
                    text = "Yellow Watch Threshold: ${yellow.toInt()} mm/h",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFFEA580C)
                )
                Slider(
                    value = yellow,
                    onValueChange = {
                        yellow = it
                        if (red < yellow) red = yellow + 1f
                    },
                    valueRange = 2f..15f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFEA580C),
                        activeTrackColor = Color(0xFFEA580C)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Red threshold
                Text(
                    text = "Red Warning Threshold: ${red.toInt()} mm/h",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFFDC2626)
                )
                Slider(
                    value = red,
                    onValueChange = {
                        red = it
                        if (yellow > red) yellow = red - 1f
                    },
                    valueRange = 5f..30f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFDC2626),
                        activeTrackColor = Color(0xFFDC2626)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        NotificationHelper.sendRiskAlertNotification(
                            context = context,
                            title = "🚨 DISASTER ALERT TEST: Severe Flood Risk",
                            message = "Heavy rainfall (18mm/h) predicted in Uttarkashi. River Bhagirathi surge expected within 2 hours. Prepare for evacuation.",
                            isCritical = true
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("test_notification_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                    Text(text = "Trigger Test Notification", modifier = Modifier.padding(start = 6.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onResetData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("reset_data_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                    Text(text = "Reset Database to Seed Samples", modifier = Modifier.padding(start = 6.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(radius.toDouble(), yellow.toDouble(), red.toDouble())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Apply Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
