package com.healthpipeline.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import com.healthpipeline.data.models.*
import com.healthpipeline.viewmodels.HealthDataViewModel

@Composable
fun DashboardScreen(
    viewModel: HealthDataViewModel,
    onRequestPermissions: (Set<String>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Observe state from ViewModel
    val healthData by viewModel.healthData.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val isLoadingData by viewModel.isLoadingData.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Local UI state for Health Connect SDK
    var healthConnectStatus by remember { mutableStateOf("Checking...") }
    var isHealthConnectAvailable by remember { mutableStateOf(false) }

    // Required permissions list for the button
    val permissionsToRequest = setOf(
        HealthPermission.getReadPermission(StepsRecord::class).toString(),
        HealthPermission.getReadPermission(HeartRateRecord::class).toString(),
        HealthPermission.getReadPermission(SleepSessionRecord::class).toString(),
        HealthPermission.getReadPermission(DistanceRecord::class).toString(),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class).toString(),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class).toString()
    )

    // Check Health Connect SDK availability on startup
    LaunchedEffect(Unit) {
        try {
            val availabilityStatus = HealthConnectClient.getSdkStatus(context)
            when (availabilityStatus) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    healthConnectStatus = "Available"
                    isHealthConnectAvailable = true
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    healthConnectStatus = "Available (Update Recommended)"
                    isHealthConnectAvailable = true
                }
                else -> {
                    healthConnectStatus = "Not Available"
                    isHealthConnectAvailable = false
                }
            }
        } catch (e: Exception) {
            healthConnectStatus = "Error: ${e.message}"
            isHealthConnectAvailable = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Health Pipeline Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Overall Health Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    healthData.healthInsights.overallHealthScore >= 85 -> MaterialTheme.colorScheme.primaryContainer
                    healthData.healthInsights.overallHealthScore >= 70 -> MaterialTheme.colorScheme.secondaryContainer
                    healthData.healthInsights.overallHealthScore >= 55 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Overall Health Score",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = healthData.healthInsights.activityLevel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${healthData.healthInsights.overallHealthScore}/100",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { healthData.healthInsights.overallHealthScore / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trend: ${healthData.healthInsights.healthTrend}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Goal: ${healthData.healthInsights.fitnessGoalProgress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Achievements & Recommendations
        if (healthData.healthInsights.achievements.isNotEmpty() || healthData.healthInsights.recommendations.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (healthData.healthInsights.achievements.isNotEmpty()) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🏆 Achievements", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            healthData.healthInsights.achievements.forEach { achievement ->
                                Text(achievement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                if (healthData.healthInsights.recommendations.isNotEmpty()) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("💡 Tips", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            healthData.healthInsights.recommendations.take(2).forEach { recommendation ->
                                Text(recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
        
        // Health Connect Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Health Connect Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusText, statusColor) = when {
                        healthConnectStatus.contains("Available") && hasPermissions -> "● CONNECTED" to MaterialTheme.colorScheme.primary
                        healthConnectStatus.contains("Available") && !hasPermissions -> "● PERMISSIONS NEEDED" to MaterialTheme.colorScheme.error
                        healthConnectStatus == "Checking..." -> "● CHECKING..." to MaterialTheme.colorScheme.outline
                        else -> "● NOT AVAILABLE" to MaterialTheme.colorScheme.error
                    }
                    Text(text = statusText, color = statusColor)
                }
                
                if (permissionStatus != "All permissions granted" && permissionStatus != "Checking...") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Status: $permissionStatus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        if (permissionStatus.contains("grant permissions in Health Connect", ignoreCase = true)) {
                            onOpenSettings()
                        } else {
                            onRequestPermissions(permissionsToRequest)
                        }
                    },
                    enabled = isHealthConnectAvailable && !hasPermissions
                ) {
                    Text(
                        when {
                            !isHealthConnectAvailable -> "Health Connect Unavailable"
                            hasPermissions -> "Permissions Granted"
                            else -> "Grant Permissions"
                        }
                    )
                }
            }
        }
        
        // Cloud Sync Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cloud Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = cloudSyncStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                cloudSyncStatus.contains("successfully") -> MaterialTheme.colorScheme.primary
                                cloudSyncStatus.contains("failed") -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Button(
                        onClick = { viewModel.syncData() },
                        enabled = hasPermissions && healthData.steps > 0 && !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onTertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Sync to Cloud")
                    }
                }
            }
        }
        
        // Quick Actions
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadHealthData() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasPermissions && !isLoadingData
                ) {
                    if (isLoadingData) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Refresh Health Data")
                }
            }
        }
        
        // Basic Metrics (Steps & HR)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${healthData.steps}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Steps Today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (healthData.heartRateZones.averageHR > 0) "${healthData.heartRateZones.averageHR}" else "--", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(text = "Avg Heart Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Distance & Calories
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (healthData.distanceKm > 0) String.format("%.1f", healthData.distanceKm) else "0.0", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text(text = "Distance (km)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${healthData.caloriesBurned}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                    Text(text = "Calories Burned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Heart Rate Zones
        if (healthData.heartRateZones.averageHR > 0) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Heart Rate Zones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${healthData.heartRateZones.restingMinutes}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Resting", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${healthData.heartRateZones.fatBurnMinutes}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Fat Burn", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${healthData.heartRateZones.cardioMinutes}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Cardio", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${healthData.heartRateZones.peakMinutes}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Peak", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        // Recent Activity Details
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏃 Recent Activity Details", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Row 1: Activity & Duration
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ActivityStatColumn(
                        label = "Activity", 
                        // Note: If your HealthData doesn't have these exact variables yet, replace with placeholder strings like "Walking" temporarily
                        value = "Walking" 
                    )
                    ActivityStatColumn(
                        label = "Duration", 
                        value = "45 min" 
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 2: Timing
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ActivityStatColumn(label = "Start Time", value = "08:30 AM")
                    ActivityStatColumn(label = "End Time", value = "09:15 AM")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 3: Intensity & Speed
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ActivityStatColumn(label = "Active Zone", value = "30 min")
                    ActivityStatColumn(label = "Speed", value = "1.4 m/s")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Row 4: Elevation
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    ActivityStatColumn(label = "Elevation Gain", value = "120 m")
                }
            }
        }
        // Sleep Analysis
        if (healthData.sleepAnalysis.totalSleepHours > 0) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sleep Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Card(colors = CardDefaults.cardColors(containerColor = if (healthData.sleepAnalysis.sleepQualityScore >= 80) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)) {
                            Text("${healthData.sleepAnalysis.sleepQualityScore}%", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${String.format("%.1f", healthData.sleepAnalysis.totalSleepHours)}h", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Total Sleep", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${healthData.sleepAnalysis.sleepEfficiency}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text("Efficiency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Status Message at bottom
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        hasPermissions && !isLoadingData -> "Health data loaded successfully"
                        hasPermissions && isLoadingData -> "Loading health data..."
                        isHealthConnectAvailable -> "Grant permissions to continue"
                        else -> "Health Connect is not available"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp)) // Bottom padding
    }
}

@Composable
fun ActivityStatColumn(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}