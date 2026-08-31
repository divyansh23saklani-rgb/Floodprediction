package com.example.flood.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flood.ui.components.OsmMapView
import com.example.flood.ui.components.OsmTileMode
import com.example.flood.ui.components.ReportIncidentSheet
import com.example.flood.ui.components.SettingsDialog
import com.example.flood.ui.components.SimulationBar
import com.example.flood.ui.components.WeatherAlertBanner
import com.example.flood.viewmodel.FloodViewModel

@Composable
fun MainMapScreen(
    viewModel: FloodViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weather by viewModel.weatherState.collectAsStateWithLifecycle()
    val incidentsList by viewModel.incidents.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showReportSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLayersMenu by remember { mutableStateOf(false) }

    var tileMode by remember { mutableStateOf(OsmTileMode.STANDARD) }
    var zoomInTrigger by remember { mutableLongStateOf(0L) }
    var zoomOutTrigger by remember { mutableLongStateOf(0L) }
    var recenterTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // High-Performance Native OpenStreetMap Engine (100% Reliable, Zero WebViews)
        OsmMapView(
            userLat = uiState.userLat,
            userLng = uiState.userLng,
            alertRadiusKm = uiState.alertRadiusKm,
            riskColor = weather.riskLevel.colorHex.let {
                when {
                    it.contains("DC2626", ignoreCase = true) -> "red"
                    it.contains("EA580C", ignoreCase = true) || it.contains("EAB308", ignoreCase = true) -> "yellow"
                    else -> "green"
                }
            },
            incidents = incidentsList,
            emergencyServices = viewModel.emergencyServices,
            showIncidents = uiState.showIncidents,
            showEmergency = uiState.showEmergency,
            showRiskZone = uiState.showRiskZone,
            tileMode = tileMode,
            zoomInTrigger = zoomInTrigger,
            zoomOutTrigger = zoomOutTrigger,
            recenterTrigger = recenterTrigger,
            isRaining = uiState.isSimulatingRain || weather.precipitationMm > 5.0,
            rainIntensity = uiState.selectedSimulation?.rainIntensity ?: weather.precipitationMm.toInt().coerceIn(10, 100),
            onMapClick = { lat, lng ->
                viewModel.onMapClick(lat, lng)
            },
            onDirections = { lat, lng ->
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Emergency Location)")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Overlay Header: Simulation playback bar and Weather Alert Card
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            SimulationBar(
                simulations = viewModel.simulations,
                activeSimulation = uiState.selectedSimulation,
                onSelectSimulation = { simId ->
                    viewModel.setSimulation(simId)
                }
            )

            WeatherAlertBanner(
                weather = weather,
                alertRadiusKm = uiState.alertRadiusKm,
                isSimulationActive = uiState.selectedSimulation != null
            )
        }

        // Single Unified Floating Control Toolbar (Right-side, Vertically Centered with Zero Collisions)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                // 1. Layer & Style Toggle
                IconButton(
                    onClick = { showLayersMenu = !showLayersMenu },
                    modifier = Modifier.size(42.dp).testTag("layer_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Layers & Style",
                        tint = if (showLayersMenu) Color(0xFF38BDF8) else Color.White
                    )
                }

                // 2. Recenter GPS
                IconButton(
                    onClick = {
                        viewModel.setUserLocation(30.7268, 78.4350)
                        recenterTrigger = System.currentTimeMillis()
                    },
                    modifier = Modifier.size(42.dp).testTag("recenter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on Location",
                        tint = Color(0xFF10B981)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(Color(0xFF334155))
                )

                // 3. Zoom In (+)
                IconButton(
                    onClick = { zoomInTrigger = System.currentTimeMillis() },
                    modifier = Modifier.size(42.dp).testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White
                    )
                }

                // 4. Zoom Out (-)
                IconButton(
                    onClick = { zoomOutTrigger = System.currentTimeMillis() },
                    modifier = Modifier.size(42.dp).testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(Color(0xFF334155))
                )

                // 5. Settings Dialog
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(42.dp).testTag("map_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Unified Map Layers & Map Style Selector Dialog
        if (showLayersMenu) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 68.dp)
                    .width(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Map Customization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { showLayersMenu = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "BASE MAP TYPE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OsmTileMode.values().forEach { mode ->
                        val isSelected = tileMode == mode
                        Surface(
                            onClick = { tileMode = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (mode) {
                                        OsmTileMode.STANDARD -> "🗺️  "
                                        OsmTileMode.TOPO -> "🏔️  "
                                        OsmTileMode.HUMANITARIAN -> "🏕️  "
                                    },
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "DISASTER OVERLAYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LayerToggleRow(
                        label = "⚠️ Incidents & Hazards",
                        checked = uiState.showIncidents,
                        onToggle = { viewModel.toggleShowIncidents() }
                    )
                    LayerToggleRow(
                        label = "🏥 Emergency Centers",
                        checked = uiState.showEmergency,
                        onToggle = { viewModel.toggleShowEmergency() }
                    )
                    LayerToggleRow(
                        label = "⭕ Danger Risk Buffer",
                        checked = uiState.showRiskZone,
                        onToggle = { viewModel.toggleShowRiskZone() }
                    )
                }
            }
        }

        // Pinned Location Action Card (appears when user clicks map)
        AnimatedVisibility(
            visible = uiState.isCustomLocationPicked && uiState.pickedLocation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .padding(horizontal = 16.dp)
        ) {
            uiState.pickedLocation?.let { loc ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Location Selected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Lat: ${String.format("%.4f", loc.first)}, Lng: ${String.format("%.4f", loc.second)}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.clearPickedLocation() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showReportSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("report_pinned_location_button")
                            ) {
                                Text("Report Here", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Primary FAB: Report Disaster / Incident
        FloatingActionButton(
            onClick = { showReportSheet = true },
            containerColor = Color(0xFFDC2626),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 16.dp)
                .testTag("fab_report_incident")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Report Incident", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Snackbar host for notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }

    // Bottom Sheet: Report Incident
    if (showReportSheet) {
        ReportIncidentSheet(
            onDismiss = { showReportSheet = false },
            onSubmit = { type, note, severity ->
                viewModel.addIncident(type, note, null, null, severity)
                showReportSheet = false
            },
            pickedLocation = uiState.pickedLocation,
            userLocation = Pair(uiState.userLat, uiState.userLng)
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentRadiusKm = uiState.alertRadiusKm,
            currentYellowThreshold = uiState.yellowThresholdMm,
            currentRedThreshold = uiState.redThresholdMm,
            onSave = { r, y, red ->
                viewModel.setAlertRadius(r)
                viewModel.setThresholds(y, red)
            },
            onResetData = {
                viewModel.resetToDefaults()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun LayerToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        color = if (checked) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                color = if (checked) Color(0xFF38BDF8) else Color(0xFF94A3B8)
            )
            Text(
                text = if (checked) "ON" else "OFF",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (checked) Color(0xFF38BDF8) else Color(0xFF64748B),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
