package com.healthpipeline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthpipeline.viewmodels.HealthDataViewModel

@Composable
fun ActivityScreen(
    viewModel: HealthDataViewModel,
    modifier: Modifier = Modifier
) {
    val healthData by viewModel.healthData.collectAsState()
    val sleep = healthData.sleepAnalysis

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C1C1E), Color(0xFF121212))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Activity Deep Dive",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 1. Active Zone Rings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                        // Peak Ring
                        CircularProgressIndicator(
                            progress = { healthData.totalActiveMinutes / 60f },
                            color = Color(0xFFFF7F50),
                            strokeWidth = 12.dp,
                            modifier = Modifier.size(180.dp)
                        )
                        // Cardio Ring
                        CircularProgressIndicator(
                            progress = { healthData.totalActiveMinutes / 90f },
                            color = Color(0xFFFF2D55),
                            strokeWidth = 12.dp,
                            modifier = Modifier.size(140.dp)
                        )
                        // Fat Burn Ring
                        CircularProgressIndicator(
                            progress = { healthData.totalActiveMinutes / 120f },
                            color = Color(0xFF5856D6),
                            strokeWidth = 12.dp,
                            modifier = Modifier.size(100.dp)
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${healthData.totalActiveMinutes}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("AZM", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ZoneLegend("Peak", Color(0xFFFF7F50))
                        ZoneLegend("Cardio", Color(0xFFFF2D55))
                        ZoneLegend("Fat Burn", Color(0xFF5856D6))
                    }
                }
            }

            // 2. Sleep Stages Breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Sleep Stages", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    SleepStageRow("Awake", sleep.awakeMinutes, sleep.awakePercent, Color(0xFFFF9500))
                    SleepStageRow("REM", sleep.remSleepMinutes, sleep.remSleepPercent, Color(0xFFAF52DE))
                    SleepStageRow("Light", sleep.lightSleepMinutes, sleep.lightSleepPercent, Color(0xFF007AFF))
                    SleepStageRow("Deep", sleep.deepSleepMinutes, sleep.deepSleepPercent, Color(0xFF5856D6))
                }
            }
            
            // 3. Activity Sessions List
            Text(
                text = "Recent Sessions",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (healthData.exerciseSessionsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No activities recorded today", color = Color.Gray)
                }
            } else {
                healthData.exerciseSessionsList.forEach { session ->
                    ActivitySessionCard(session)
                }
            }
            
            // 4. System Info
            Text(
                text = "System ID: ${healthData.exerciseSessionsList.firstOrNull()?.pseudoId2 ?: "VITAL_USER_PENDING"}",
                fontSize = 10.sp,
                color = Color.DarkGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ActivitySessionCard(session: com.healthpipeline.data.models.ExerciseSessionDetails) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(session.activityName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text("${session.startTime} - ${session.endTime}", color = Color.Gray, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SessionMiniMetric("Dist", String.format("%.1f km", session.distance))
                SessionMiniMetric("Dur", "${session.duration}m")
                SessionMiniMetric("HR", "${session.averageHeartRate} bpm")
                SessionMiniMetric("Kcal", "${session.calories}")
            }
        }
    }
}

@Composable
fun SessionMiniMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun ZoneLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun SleepStageRow(label: String, minutes: Int?, percent: Double?, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp)
            val displayText = if (minutes != null && percent != null) "${minutes}m (${String.format("%.0f%%", percent)})" else "--"
            Text(displayText, color = Color.Gray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (percent ?: 0.0 / 100f).toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.05f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
