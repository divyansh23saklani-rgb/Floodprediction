package com.example.flood.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.flood.data.model.EmergencyService
import com.example.flood.data.model.Incident
import com.example.flood.data.model.ServiceType
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import kotlin.random.Random

sealed class OsmSelectedItem {
    data class IncidentItem(val incident: Incident) : OsmSelectedItem()
    data class ServiceItem(val service: EmergencyService) : OsmSelectedItem()
}

enum class OsmTileMode(val label: String) {
    STANDARD("Standard Map"),
    TOPO("Topographic & Terrain"),
    HUMANITARIAN("Relief & Infrastructure")
}

@Composable
fun OsmMapView(
    userLat: Double,
    userLng: Double,
    alertRadiusKm: Double,
    riskColor: String, // red, orange, yellow, green
    incidents: List<Incident>,
    emergencyServices: List<EmergencyService>,
    showIncidents: Boolean,
    showEmergency: Boolean,
    showRiskZone: Boolean,
    tileMode: OsmTileMode = OsmTileMode.STANDARD,
    zoomInTrigger: Long = 0L,
    zoomOutTrigger: Long = 0L,
    recenterTrigger: Long = 0L,
    isRaining: Boolean,
    rainIntensity: Int,
    onMapClick: (lat: Double, lng: Double) -> Unit,
    onDirections: (lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedItem by remember { mutableStateOf<OsmSelectedItem?>(null) }

    // Initialize osmdroid configuration using internal app cache for maximum portability across all devices
    val mapView = remember {
        val basePath = File(context.cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = tileCache
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(userLat, userLng))
        }
    }

    // Manage MapView Lifecycle (onResume / onPause / onDetach)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Switch Tile Source when mode changes
    LaunchedEffect(tileMode) {
        when (tileMode) {
            OsmTileMode.STANDARD -> mapView.setTileSource(TileSourceFactory.MAPNIK)
            OsmTileMode.TOPO -> mapView.setTileSource(TileSourceFactory.OpenTopo)
            OsmTileMode.HUMANITARIAN -> mapView.setTileSource(TileSourceFactory.HIKEBIKEMAP)
        }
        mapView.invalidate()
    }

    // Handle Zoom In Action
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0L) {
            mapView.controller.zoomIn()
        }
    }

    // Handle Zoom Out Action
    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0L) {
            mapView.controller.zoomOut()
        }
    }

    // Handle Recenter Action
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0L) {
            mapView.controller.animateTo(GeoPoint(userLat, userLng), 14.5, 600L)
        }
    }

    // Recenter when user location changes
    LaunchedEffect(userLat, userLng) {
        mapView.controller.animateTo(GeoPoint(userLat, userLng))
    }

    // Synchronize Overlays (Risk Zone Circle, Incidents, Emergency Services, Click Overlay)
    LaunchedEffect(
        userLat, userLng, alertRadiusKm, riskColor,
        incidents, emergencyServices, showIncidents,
        showEmergency, showRiskZone
    ) {
        mapView.overlays.clear()

        // 1. Map Click Receiver Overlay
        val clickReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selectedItem = null
                    onMapClick(p.latitude, p.longitude)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(MapEventsOverlay(clickReceiver))

        // 2. Risk Zone Buffer Circle Overlay
        if (showRiskZone) {
            val radiusMeters = alertRadiusKm * 1000.0
            val circlePoints = Polygon.pointsAsCircle(GeoPoint(userLat, userLng), radiusMeters)
            val polygon = Polygon(mapView).apply {
                points = circlePoints
                val (fillCol, strokeCol) = when (riskColor.lowercase()) {
                    "red" -> android.graphics.Color.argb(55, 220, 38, 38) to android.graphics.Color.argb(220, 220, 38, 38)
                    "orange", "yellow" -> android.graphics.Color.argb(55, 234, 88, 12) to android.graphics.Color.argb(220, 234, 88, 12)
                    else -> android.graphics.Color.argb(45, 22, 163, 74) to android.graphics.Color.argb(200, 22, 163, 74)
                }
                fillPaint.color = fillCol
                outlinePaint.color = strokeCol
                outlinePaint.strokeWidth = 5f
            }
            mapView.overlays.add(polygon)
        }

        // 3. User Location Marker
        val userMarker = Marker(mapView).apply {
            position = GeoPoint(userLat, userLng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createUserLocationIcon(context)
            title = "My Current Location"
            setOnMarkerClickListener { _, _ -> true }
        }
        mapView.overlays.add(userMarker)

        // 4. Emergency Services Markers
        if (showEmergency) {
            emergencyServices.forEach { service ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(service.lat, service.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createEmergencyIcon(context, service)
                    title = service.name
                    snippet = service.address
                    setOnMarkerClickListener { _, _ ->
                        selectedItem = OsmSelectedItem.ServiceItem(service)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }

        // 5. Incident Hazard Markers
        if (showIncidents) {
            incidents.forEach { incident ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(incident.lat, incident.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createIncidentIcon(context, incident)
                    title = incident.incidentType.label
                    snippet = incident.note
                    setOnMarkerClickListener { _, _ ->
                        selectedItem = OsmSelectedItem.IncidentItem(incident)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }

        mapView.invalidate()
    }

    // Infinite animation for rain simulation overlay
    val infiniteTransition = rememberInfiniteTransition(label = "osm_rain")
    val rainOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_drop_fall"
    )
    val rainDrops = remember {
        List(140) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 22f + 14f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Native OpenStreetMap View Container
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Rain Simulation Overlay Canvas
        if (isRaining) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dropCount = (rainIntensity * 1.3f).toInt().coerceIn(30, rainDrops.size)
                val dropColor = Color(0x990284C7)
                for (i in 0 until dropCount) {
                    val (normX, normY, length) = rainDrops[i]
                    val startX = (normX * size.width) - 25f
                    val startY = ((normY * size.height) + rainOffsetY) % size.height
                    val endX = startX - 10f
                    val endY = startY + length

                    drawLine(
                        color = dropColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Bottom Selected Marker Card Inspector (Clear of any floating buttons)
        selectedItem?.let { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                when (item) {
                    is OsmSelectedItem.IncidentItem -> {
                        val inc = item.incident
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inc.incidentType.emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inc.incidentType.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Severity: ${inc.severity.uppercase()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (inc.severity.uppercase()) {
                                            "HIGH" -> Color(0xFFEF4444)
                                            "MEDIUM" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF10B981)
                                        }
                                    )
                                }
                                IconButton(onClick = { selectedItem = null }) {
                                    Text("✕", color = Color(0xFF94A3B8), fontSize = 18.sp)
                                }
                            }
                            if (inc.note.isNotBlank()) {
                                Text(
                                    text = inc.note,
                                    fontSize = 13.sp,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lat: ${String.format("%.4f", inc.lat)}, Lng: ${String.format("%.4f", inc.lng)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onDirections(inc.lat, inc.lng) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Navigate", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    is OsmSelectedItem.ServiceItem -> {
                        val srv = item.service
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = srv.serviceType.emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = srv.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = srv.serviceType.label,
                                        fontSize = 12.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                IconButton(onClick = { selectedItem = null }) {
                                    Text("✕", color = Color(0xFF94A3B8), fontSize = 18.sp)
                                }
                            }
                            Text(
                                text = srv.address,
                                fontSize = 13.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📞 ${srv.phone}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onDirections(srv.lat, srv.lng) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Directions", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Icon Generators for Native OSM Overlays
private fun createUserLocationIcon(context: Context): Drawable {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer glow ring
    paint.color = android.graphics.Color.argb(80, 56, 189, 248)
    canvas.drawCircle(size / 2f, size / 2f, 28f, paint)

    // White border
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, 18f, paint)

    // Blue center dot
    paint.color = android.graphics.Color.parseColor("#0284C7")
    canvas.drawCircle(size / 2f, size / 2f, 14f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createIncidentIcon(context: Context, incident: Incident): Drawable {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Shadow
    paint.color = android.graphics.Color.argb(100, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f + 4f, 32f, paint)

    // Colored circle badge
    paint.color = android.graphics.Color.parseColor(incident.incidentType.colorHex)
    canvas.drawCircle(size / 2f, size / 2f, 30f, paint)

    // White rim
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawCircle(size / 2f, size / 2f, 30f, paint)

    // Text Emoji inside
    paint.style = Paint.Style.FILL
    paint.textSize = 34f
    paint.textAlign = Paint.Align.CENTER
    val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(incident.incidentType.emoji, size / 2f, yPos, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createEmergencyIcon(context: Context, service: EmergencyService): Drawable {
    val size = 76
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Shadow
    paint.color = android.graphics.Color.argb(100, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f + 3f, 30f, paint)

    // Background color
    val colorHex = when (service.serviceType) {
        ServiceType.HOSPITAL -> "#16A34A"
        ServiceType.SHELTER -> "#D97706"
        ServiceType.POLICE -> "#0284C7"
    }
    paint.color = android.graphics.Color.parseColor(colorHex)
    canvas.drawCircle(size / 2f, size / 2f, 28f, paint)

    // White rim
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3.5f
    canvas.drawCircle(size / 2f, size / 2f, 28f, paint)

    // Text Emoji inside
    paint.style = Paint.Style.FILL
    paint.textSize = 32f
    paint.textAlign = Paint.Align.CENTER
    val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(service.serviceType.emoji, size / 2f, yPos, paint)

    return BitmapDrawable(context.resources, bitmap)
}

