package com.healthpipeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthpipeline.viewmodels.HealthDataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HealthDataViewModel,
    onSignOut: () -> Unit,
    onRequestPermissions: (Set<String>) -> Unit,
    onOpenSettings: () -> Unit,
    userId: String?,
    modifier: Modifier = Modifier
) {
    val healthData by viewModel.healthData.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    val backgroundColorTop = Color(0xFF20232B)
    val backgroundColorBottom = Color(0xFF111216)
    val cardColor = Color.White.copy(alpha = 0.05f)
    val accentColor = Color(0xFFFF6B6B)
    val cardBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(backgroundColorTop, backgroundColorBottom)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, User",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = cloudSyncStatus,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor, strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.syncData(userId) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = accentColor)
                            }
                        }
                        
                        IconButton(onClick = onSignOut) {
                            Text("Exit", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                if (!hasPermissions) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Permissions Required", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("PHIA needs access to your Health Connect data to provide insights.", color = Color.LightGray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    Log.d("DashboardScreen", "🔘 Grant Access button clicked - calling onRequestPermissions")
                                    onRequestPermissions(emptySet())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Grant Access")
                            }
                        }
                    }
                }

                // 1. Stress Management Score (High Glow Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = cardBorder
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stress Score", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                text = healthData.vitals.stressManagementScore?.toString() ?: "--",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                            CircularProgressIndicator(
                                progress = { (healthData.vitals.stressManagementScore ?: 0) / 100f },
                                color = accentColor,
                                trackColor = Color.White.copy(alpha = 0.05f),
                                strokeWidth = 6.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 2. Vitals Row (RHR & HRV)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricGlassCard(
                        modifier = Modifier.weight(1f),
                        label = "Resting HR",
                        value = healthData.vitals.restingHeartRate?.toString() ?: "--",
                        unit = "bpm",
                        icon = "💓",
                        color = Color(0xFFFF4D4D),
                        cardColor = cardColor,
                        cardBorder = cardBorder
                    )
                    MetricGlassCard(
                        modifier = Modifier.weight(1f),
                        label = "HRV",
                        value = healthData.vitals.heartRateVariability?.let { String.format("%.0f", it) } ?: "--",
                        unit = "ms",
                        icon = "📉",
                        color = Color(0xFF6B8AFF),
                        cardColor = cardColor,
                        cardBorder = cardBorder
                    )
                }

                // 3. Activity Row (Steps & Calories)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricGlassCard(
                        modifier = Modifier.weight(1f),
                        label = "Steps",
                        value = "${healthData.totalSteps}",
                        unit = "",
                        icon = "👣",
                        color = Color(0xFF4DFFB8),
                        cardColor = cardColor,
                        cardBorder = cardBorder
                    )
                    MetricGlassCard(
                        modifier = Modifier.weight(1f),
                        label = "Calories",
                        value = "${healthData.totalCaloriesBurned}",
                        unit = "kcal",
                        icon = "🔥",
                        color = Color(0xFFFFB84D),
                        cardColor = cardColor,
                        cardBorder = cardBorder
                    )
                }

                // 4. Extended Metrics (Distance, Speed, Active Minutes)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricGlassCard(
                        modifier = Modifier.weight(1f),
                        label = "Distance",
                        value = String.format("%.1f", healthData.totalDistanceKm),
                        unit = "km",
                        icon = "📍",
                        color = Color(0xFF6B8AFF),
                        cardColor = cardColor,
                        cardBorder = cardBorder
                    )
                    MetricGlassCard(
                        modifier = Modifier.weight(1f),
                        label = "Active",
                        value = "${healthData.totalActiveMinutes}",
                        unit = "min",
                        icon = "⚡",
                        color = Color(0xFFFF6B6B),
                        cardColor = cardColor,
                        cardBorder = cardBorder
                    )
                }

                // 5. Zone Intensity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Intensity Zones", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val session = healthData.exerciseSessionsList.firstOrNull()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ZoneMiniMetric("Fat Burn", "${session?.fatburnActiveZoneMinutes ?: 0}m", Color(0xFF5856D6))
                            ZoneMiniMetric("Cardio", "${session?.cardioActiveZoneMinutes ?: 0}m", Color(0xFFFF2D55))
                            ZoneMiniMetric("Peak", "${session?.peakActiveZoneMinutes ?: 0}m", Color(0xFFFF7F50))
                        }
                    }
                }

                // 6. Sleep Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Sleep Summary", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SleepMiniMetric("Duration", healthData.sleepAnalysis.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "--")
                            SleepMiniMetric("Deep Sleep", healthData.sleepAnalysis.deepSleepMinutes?.let { "${it}m" } ?: "--")
                            val totalPercent = listOf(healthData.sleepAnalysis.deepSleepPercent, healthData.sleepAnalysis.remSleepPercent, healthData.sleepAnalysis.lightSleepPercent).filterNotNull().sum()
                            SleepMiniMetric("Efficiency", if (totalPercent > 0) String.format("%.0f%%", totalPercent) else "--")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom nav
            }
        }
    }
}

@Composable
fun MetricGlassCard(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    icon: String,
    color: Color,
    cardColor: Color,
    cardBorder: BorderStroke
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                if (unit.isNotEmpty()) {
                    Text(text = unit, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SleepMiniMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun ZoneMiniMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
