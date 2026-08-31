package com.example.flood

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flood.ui.screens.EmergencyServicesScreen
import com.example.flood.ui.screens.IncidentListScreen
import com.example.flood.ui.screens.MainMapScreen
import com.example.flood.ui.screens.WeatherDetailsScreen
import com.example.flood.ui.theme.FloodAlertTheme
import com.example.flood.util.NotificationHelper
import com.example.flood.viewmodel.FloodViewModel
import org.osmdroid.config.Configuration

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Map : Screen(
        route = "map",
        title = "Live Map",
        icon = { Icon(imageVector = Icons.Default.Map, contentDescription = "Map", modifier = Modifier.size(22.dp)) }
    )
    object Incidents : Screen(
        route = "incidents",
        title = "Hazards",
        icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = "Incidents", modifier = Modifier.size(22.dp)) }
    )
    object Emergency : Screen(
        route = "emergency",
        title = "Shelters",
        icon = { Icon(imageVector = Icons.Default.LocalHospital, contentDescription = "Emergency", modifier = Modifier.size(22.dp)) }
    )
    object Weather : Screen(
        route = "weather",
        title = "Forecast",
        icon = { Icon(imageVector = Icons.Default.Cloud, contentDescription = "Weather", modifier = Modifier.size(22.dp)) }
    )
}

class MainActivity : ComponentActivity() {
    private val viewModel: FloodViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OsmDroid native map engine
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        
        // Setup Disaster & Warning Notification Channels
        NotificationHelper.createNotificationChannels(this)

        enableEdgeToEdge()

        setContent {
            FloodAlertTheme {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Permission granted/denied handled gracefully */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val navController = rememberNavController()
                val screens = listOf(Screen.Map, Screen.Incidents, Screen.Emergency, Screen.Weather)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            screens.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = screen.icon,
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0284C7),
                                        selectedTextColor = Color(0xFF0284C7),
                                        indicatorColor = Color(0xFFE0F2FE),
                                        unselectedIconColor = Color(0xFF64748B),
                                        unselectedTextColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Map.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Map.route) {
                            MainMapScreen(
                                viewModel = viewModel
                            )
                        }
                        composable(Screen.Incidents.route) {
                            IncidentListScreen(
                                viewModel = viewModel,
                                onNavigateToMapTarget = { lat, lng ->
                                    viewModel.setUserLocation(lat, lng)
                                    navController.navigate(Screen.Map.route)
                                }
                            )
                        }
                        composable(Screen.Emergency.route) {
                            EmergencyServicesScreen(
                                viewModel = viewModel
                            )
                        }
                        composable(Screen.Weather.route) {
                            WeatherDetailsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
