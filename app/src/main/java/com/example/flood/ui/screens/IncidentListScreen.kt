package com.example.flood.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flood.data.model.Comment
import com.example.flood.data.model.Incident
import com.example.flood.data.model.IncidentType
import com.example.flood.ui.components.IncidentCommentsSheet
import com.example.flood.ui.components.ReportIncidentSheet
import com.example.flood.viewmodel.FloodViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    viewModel: FloodViewModel,
    onNavigateToMapTarget: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    val allComments by viewModel.comments.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedStatusFilter by remember { mutableStateOf("OPEN") } // OPEN, ALL, RESOLVED
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showReportSheet by remember { mutableStateOf(false) }
    var selectedIncidentForComments by remember { mutableStateOf<Incident?>(null) }

    val openCount = remember(incidents) { incidents.count { it.isOpen } }
    val resolvedCount = remember(incidents) { incidents.count { !it.isOpen } }

    val filteredIncidents = remember(incidents, selectedStatusFilter, selectedTypeFilter) {
        incidents.filter { inc ->
            val statusMatch = when (selectedStatusFilter) {
                "OPEN" -> inc.isOpen
                "RESOLVED" -> !inc.isOpen
                else -> true
            }
            val typeMatch = if (selectedTypeFilter == "ALL") true else inc.type.equals(selectedTypeFilter, ignoreCase = true)
            statusMatch && typeMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Community Hazard Network",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$openCount Open Hazards • Live Sync Active",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.testTag("sync_incidents_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Community Hazards",
                            tint = Color(0xFF0284C7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showReportSheet = true },
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_incident_list")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Report Hazard", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Status Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == "OPEN",
                    onClick = { selectedStatusFilter = "OPEN" },
                    label = { Text("🚨 Open ($openCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_open_hazards")
                )

                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { selectedStatusFilter = "ALL" },
                    label = { Text("All (${incidents.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_all_hazards")
                )

                FilterChip(
                    selected = selectedStatusFilter == "RESOLVED",
                    onClick = { selectedStatusFilter = "RESOLVED" },
                    label = { Text("✅ Resolved ($resolvedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF16A34A),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_resolved_hazards")
                )
            }

            // Category Type Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "ALL",
                        onClick = { selectedTypeFilter = "ALL" },
                        label = { Text("All Types") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF334155),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9)
                        )
                    )
                }

                items(IncidentType.entries) { type ->
                    val count = incidents.count { it.type.equals(type.name, ignoreCase = true) }
                    FilterChip(
                        selected = selectedTypeFilter.equals(type.name, ignoreCase = true),
                        onClick = { selectedTypeFilter = type.name },
                        label = { Text("${type.emoji} ${type.label} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9)
                        )
                    )
                }
            }

            if (filteredIncidents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedStatusFilter == "OPEN") "No open hazards in this category 🎉" else "No incidents found",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Community safety is clear or filter is restricted.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredIncidents, key = { it.id }) { incident ->
                        val commentsCount = allComments.count { it.incidentId == incident.id || (incident.createdAt != 0L && it.incidentCreatedAt == incident.createdAt) }
                        IncidentItemCard(
                            incident = incident,
                            commentsCount = commentsCount,
                            onUpvote = { viewModel.upvoteIncident(incident.id) },
                            onDownvote = { viewModel.downvoteIncident(incident.id) },
                            onToggleStatus = {
                                val nextStatus = if (incident.isOpen) "RESOLVED" else "OPEN"
                                viewModel.updateIncidentStatus(incident.id, nextStatus)
                            },
                            onOpenComments = { selectedIncidentForComments = incident },
                            onDelete = { viewModel.deleteIncident(incident.id) },
                            onNavigate = {
                                val uri = Uri.parse("geo:${incident.lat},${incident.lng}?q=${incident.lat},${incident.lng}(${incident.type})")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${incident.lat},${incident.lng}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (showReportSheet) {
        ReportIncidentSheet(
            onDismiss = { showReportSheet = false },
            onSubmit = { type, note, severity ->
                viewModel.addIncident(type, note, null, null, severity)
                showReportSheet = false
            },
            pickedLocation = null,
            userLocation = Pair(uiState.userLat, uiState.userLng)
        )
    }

    selectedIncidentForComments?.let { incident ->
        IncidentCommentsSheet(
            incident = incident,
            viewModel = viewModel,
            onDismiss = { selectedIncidentForComments = null }
        )
    }
}

@Composable
private fun IncidentItemCard(
    incident: Incident,
    commentsCount: Int,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onToggleStatus: () -> Unit,
    onOpenComments: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val incidentType = incident.incidentType
    val timeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(incident.createdAt))

    val severityColor = when (incident.severity.uppercase()) {
        "HIGH" -> Color(0xFFDC2626)
        "MEDIUM" -> Color(0xFFEA580C)
        else -> Color(0xFF16A34A)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("incident_card_${incident.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Emoji, Label, Status, and Severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = incidentType.emoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = incidentType.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (incident.isOpen) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                modifier = Modifier.clickable { onToggleStatus() }
                            ) {
                                Text(
                                    text = if (incident.isOpen) "🚨 OPEN" else "✅ RESOLVED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (incident.isOpen) Color(0xFFDC2626) else Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = severityColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = incident.severity,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (incident.note.isNotBlank()) {
                Text(
                    text = incident.note,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Coordinates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${String.format("%.4f", incident.lat)}, ${String.format("%.4f", incident.lng)}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete report",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onNavigate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Voting, Verification & Comments Action Bar
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Upvote & Downvote Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Upvote Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (incident.userVote == 1) Color(0xFFE0F2FE) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpvote() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("upvote_button_${incident.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (incident.userVote == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Upvote / Verify",
                                    tint = if (incident.userVote == 1) Color(0xFF0284C7) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${incident.upvotes}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (incident.userVote == 1) Color(0xFF0284C7) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Downvote Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (incident.userVote == -1) Color(0xFFFEE2E2) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDownvote() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("downvote_button_${incident.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (incident.userVote == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                    contentDescription = "Downvote / Dispute",
                                    tint = if (incident.userVote == -1) Color(0xFFDC2626) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${incident.downvotes}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (incident.userVote == -1) Color(0xFFDC2626) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Community Verification Score Badge
                        if (incident.score > 0) {
                            Text(
                                text = "+${incident.score} Verified",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        } else if (incident.score < 0) {
                            Text(
                                text = "${incident.score} Disputed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    // Comments Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenComments() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("open_comments_button_${incident.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (commentsCount > 0) "$commentsCount Comments" else "Comment",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}
