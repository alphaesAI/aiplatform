package com.healthpipeline

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.healthpipeline.ui.theme.HealthPipelineTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(this) }
    
    // Health Connect permissions
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )
    
    // Permission launcher
    private val requestPermissionActivityContract = 
        PermissionController.createRequestPermissionResultContract()
    
    private lateinit var requestPermissions: androidx.activity.result.ActivityResultLauncher<Set<String>>
    
    // Health data classes
    data class HealthInsights(
        val overallHealthScore: Int = 0,
        val activityLevel: String = "Unknown",
        val fitnessGoalProgress: Int = 0,
        val healthTrend: String = "Stable",
        val recommendations: List<String> = emptyList(),
        val achievements: List<String> = emptyList()
    )
    
    data class SleepAnalysis(
        val totalSleepHours: Double = 0.0,
        val deepSleepMinutes: Int = 0,
        val lightSleepMinutes: Int = 0,
        val remSleepMinutes: Int = 0,
        val awakeMinutes: Int = 0,
        val sleepEfficiency: Int = 0,
        val sleepQualityScore: Int = 0,
        val bedTime: String = "No data",
        val wakeTime: String = "No data"
    )
    
    data class HeartRateZones(
        val restingMinutes: Int = 0,
        val fatBurnMinutes: Int = 0,
        val cardioMinutes: Int = 0,
        val peakMinutes: Int = 0,
        val restingHR: Int = 0,
        val maxHR: Int = 0,
        val averageHR: Int = 0
    )
    
    data class HealthData(
        val steps: Int = 0,
        val heartRate: Int = 0,
        val sleepHours: Double = 0.0,
        val distanceKm: Double = 0.0,
        val caloriesBurned: Int = 0,
        val exerciseSessions: Int = 0,
        val activeMinutes: Int = 0,
        val lastExerciseType: String = "None",
        val heartRateZones: HeartRateZones = HeartRateZones(),
        val sleepAnalysis: SleepAnalysis = SleepAnalysis(),
        val healthInsights: HealthInsights = HealthInsights()
    )
    
    // Move checkPermissions to class level
    private suspend fun checkPermissions(): Pair<String, Boolean> {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            val hasAllPermissions = permissions.all { it in grantedPermissions }
            
            if (hasAllPermissions) {
                "All permissions granted" to true
            } else {
                "Permissions needed" to false
            }
        } catch (e: Exception) {
            "Error: ${e.message}" to false
        }
    }
    
    // Read health data from Health Connect
    private suspend fun readHealthData(): HealthData {
        return try {
            val steps = readTodaySteps()
            val heartRate = readTodayHeartRate()
            val sleep = readLastNightSleep()
            val distance = readTodayDistance()
            val calories = readTodayCalories()
            val (exerciseSessions, activeMinutes, lastExercise) = readTodayExercise()
            val heartRateZones = readHeartRateZones()
            val sleepAnalysis = readSleepAnalysis()
            
            // Calculate health insights based on all data
            val healthInsights = calculateHealthInsights(steps, heartRateZones, sleepAnalysis, exerciseSessions, activeMinutes, calories)
            
            HealthData(steps, heartRate, sleep, distance, calories, exerciseSessions, activeMinutes, lastExercise, heartRateZones, sleepAnalysis, healthInsights)
        } catch (e: Exception) {
            HealthData() // Return empty data on error
        }
    }
    
    private suspend fun readTodaySteps(): Int {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.toInstant()
            
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.count.toInt() }
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readTodayHeartRate(): Int {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.toInstant()
            
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            if (response.records.isNotEmpty()) {
                val allSamples = response.records.flatMap { it.samples }
                if (allSamples.isNotEmpty()) {
                    allSamples.map { it.beatsPerMinute.toInt() }.average().toInt()
                } else 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readLastNightSleep(): Double {
        return try {
            val now = Instant.now()
            val yesterday = now.minus(1, ChronoUnit.DAYS)
            
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(yesterday, now)
            )
            
            val response = healthConnectClient.readRecords(request)
            if (response.records.isNotEmpty()) {
                val latestSleep = response.records.maxByOrNull { it.startTime }
                latestSleep?.let {
                    val durationMillis = it.endTime.toEpochMilli() - it.startTime.toEpochMilli()
                    durationMillis / (1000.0 * 60 * 60) // Convert to hours
                } ?: 0.0
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    private suspend fun readTodayDistance(): Double {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.toInstant()
            
            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            val totalMeters = response.records.sumOf { it.distance.inMeters }
            totalMeters / 1000.0 // Convert to kilometers
        } catch (e: Exception) {
            0.0
        }
    }
    
    private suspend fun readTodayCalories(): Int {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.toInstant()
            
            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.energy.inCalories }.toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readTodayExercise(): Triple<Int, Int, String> {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.toInstant()
            
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            val sessions = response.records
            
            val sessionCount = sessions.size
            val totalActiveMinutes = sessions.sumOf { 
                val durationMillis = it.endTime.toEpochMilli() - it.startTime.toEpochMilli()
                (durationMillis / (1000 * 60)).toInt() // Convert to minutes
            }
            val lastExerciseType = sessions.lastOrNull()?.exerciseType?.toString() ?: "None"
            
            Triple(sessionCount, totalActiveMinutes, lastExerciseType)
        } catch (e: Exception) {
            Triple(0, 0, "None")
        }
    }
    
    private suspend fun readHeartRateZones(): HeartRateZones {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.toInstant()
            
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            val allSamples = response.records.flatMap { it.samples }
            
            if (allSamples.isEmpty()) {
                return HeartRateZones()
            }
            
            val heartRates = allSamples.map { it.beatsPerMinute.toInt() }
            val minHR = heartRates.minOrNull() ?: 0
            val maxHR = heartRates.maxOrNull() ?: 0
            val avgHR = heartRates.average().toInt()
            
            // Calculate estimated max heart rate (220 - age, using 30 as default)
            val estimatedMaxHR = 190 // Assuming age ~30, can be made dynamic
            
            // Define heart rate zones based on percentage of max HR
            val restingZone = 50..60
            val fatBurnZone = (estimatedMaxHR * 0.6).toInt()..(estimatedMaxHR * 0.7).toInt()
            val cardioZone = (estimatedMaxHR * 0.7).toInt()..(estimatedMaxHR * 0.85).toInt()
            val peakZone = (estimatedMaxHR * 0.85).toInt()..estimatedMaxHR
            
            // Count minutes in each zone (approximate based on samples)
            val samplesPerMinute = if (allSamples.size > 0) allSamples.size / 60.0 else 1.0
            val minutesPerSample = 1.0 / samplesPerMinute
            
            var restingMinutes = 0
            var fatBurnMinutes = 0
            var cardioMinutes = 0
            var peakMinutes = 0
            
            heartRates.forEach { hr ->
                val minutes = minutesPerSample.toInt().coerceAtLeast(1)
                when {
                    hr in restingZone -> restingMinutes += minutes
                    hr in fatBurnZone -> fatBurnMinutes += minutes
                    hr in cardioZone -> cardioMinutes += minutes
                    hr in peakZone -> peakMinutes += minutes
                }
            }
            
            HeartRateZones(
                restingMinutes = restingMinutes,
                fatBurnMinutes = fatBurnMinutes,
                cardioMinutes = cardioMinutes,
                peakMinutes = peakMinutes,
                restingHR = minHR,
                maxHR = maxHR,
                averageHR = avgHR
            )
        } catch (e: Exception) {
            HeartRateZones()
        }
    }
    
    private suspend fun readSleepAnalysis(): SleepAnalysis {
        return try {
            val now = Instant.now()
            val yesterday = now.minus(1, ChronoUnit.DAYS)
            
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(yesterday, now)
            )
            
            val response = healthConnectClient.readRecords(request)
            if (response.records.isEmpty()) {
                return SleepAnalysis()
            }
            
            val latestSleep = response.records.maxByOrNull { it.startTime }
            if (latestSleep == null) {
                return SleepAnalysis()
            }
            
            // Calculate total sleep duration
            val durationMillis = latestSleep.endTime.toEpochMilli() - latestSleep.startTime.toEpochMilli()
            val totalSleepHours = durationMillis / (1000.0 * 60 * 60)
            
            // Format bed time and wake time
            val bedTime = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                .format(latestSleep.startTime.atZone(ZoneId.systemDefault()))
            val wakeTime = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                .format(latestSleep.endTime.atZone(ZoneId.systemDefault()))
            
            // Analyze sleep stages (if available)
            val stages = latestSleep.stages
            var deepSleepMinutes = 0
            var lightSleepMinutes = 0
            var remSleepMinutes = 0
            var awakeMinutes = 0
            
            stages.forEach { stage ->
                val stageDurationMinutes = (stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()) / (1000 * 60)
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepSleepMinutes += stageDurationMinutes.toInt()
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> lightSleepMinutes += stageDurationMinutes.toInt()
                    SleepSessionRecord.STAGE_TYPE_REM -> remSleepMinutes += stageDurationMinutes.toInt()
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeMinutes += stageDurationMinutes.toInt()
                    else -> lightSleepMinutes += stageDurationMinutes.toInt() // Default to light sleep
                }
            }
            
            // If no stages data, estimate based on typical sleep patterns
            if (deepSleepMinutes == 0 && lightSleepMinutes == 0 && remSleepMinutes == 0) {
                val totalMinutes = totalSleepHours * 60
                deepSleepMinutes = (totalMinutes * 0.20).toInt() // 20% deep sleep
                lightSleepMinutes = (totalMinutes * 0.55).toInt() // 55% light sleep
                remSleepMinutes = (totalMinutes * 0.20).toInt() // 20% REM sleep
                awakeMinutes = (totalMinutes * 0.05).toInt() // 5% awake
            }
            
            // Calculate sleep efficiency (time asleep vs time in bed)
            val timeAsleep = deepSleepMinutes + lightSleepMinutes + remSleepMinutes
            val timeInBed = timeAsleep + awakeMinutes
            val sleepEfficiency = if (timeInBed > 0) ((timeAsleep.toDouble() / timeInBed) * 100).toInt() else 0
            
            // Calculate sleep quality score (0-100)
            val sleepQualityScore = calculateSleepQuality(totalSleepHours, deepSleepMinutes, sleepEfficiency, awakeMinutes)
            
            SleepAnalysis(
                totalSleepHours = totalSleepHours,
                deepSleepMinutes = deepSleepMinutes,
                lightSleepMinutes = lightSleepMinutes,
                remSleepMinutes = remSleepMinutes,
                awakeMinutes = awakeMinutes,
                sleepEfficiency = sleepEfficiency,
                sleepQualityScore = sleepQualityScore,
                bedTime = bedTime,
                wakeTime = wakeTime
            )
        } catch (e: Exception) {
            SleepAnalysis()
        }
    }
    
    private fun calculateSleepQuality(totalHours: Double, deepMinutes: Int, efficiency: Int, awakeMinutes: Int): Int {
        var score = 50 // Base score
        
        // Duration score (7-9 hours is optimal)
        when {
            totalHours in 7.0..9.0 -> score += 25
            totalHours in 6.0..7.0 || totalHours in 9.0..10.0 -> score += 15
            totalHours in 5.0..6.0 || totalHours in 10.0..11.0 -> score += 5
            else -> score -= 10
        }
        
        // Deep sleep score (15-20% is good)
        val deepPercentage = if (totalHours > 0) (deepMinutes / (totalHours * 60)) * 100 else 0.0
        when {
            deepPercentage >= 15 -> score += 15
            deepPercentage >= 10 -> score += 10
            deepPercentage >= 5 -> score += 5
            else -> score -= 5
        }
        
        // Efficiency score
        when {
            efficiency >= 90 -> score += 10
            efficiency >= 80 -> score += 5
            efficiency >= 70 -> score += 2
            else -> score -= 5
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun calculateHealthInsights(
        steps: Int,
        heartRateZones: HeartRateZones,
        sleepAnalysis: SleepAnalysis,
        exerciseSessions: Int,
        activeMinutes: Int,
        calories: Int
    ): HealthInsights {
        var overallScore = 0
        val recommendations = mutableListOf<String>()
        val achievements = mutableListOf<String>()
        
        // Steps analysis (25 points max)
        val stepsScore = when {
            steps >= 10000 -> {
                achievements.add("🎯 Daily step goal achieved!")
                25
            }
            steps >= 7500 -> 20
            steps >= 5000 -> 15
            steps >= 2500 -> 10
            else -> {
                recommendations.add("Try to walk more - aim for 10,000 steps daily")
                5
            }
        }
        overallScore += stepsScore
        
        // Sleep analysis (25 points max)
        val sleepScore = if (sleepAnalysis.sleepQualityScore > 0) {
            (sleepAnalysis.sleepQualityScore * 0.25).toInt()
        } else {
            recommendations.add("Track your sleep for better health insights")
            10
        }
        overallScore += sleepScore
        
        // Exercise analysis (25 points max)
        val exerciseScore = when {
            activeMinutes >= 150 -> {
                achievements.add("💪 Weekly exercise goal on track!")
                25
            }
            activeMinutes >= 75 -> 20
            activeMinutes >= 30 -> 15
            exerciseSessions > 0 -> 10
            else -> {
                recommendations.add("Add 30 minutes of exercise to your day")
                5
            }
        }
        overallScore += exerciseScore
        
        // Heart rate analysis (25 points max)
        val heartRateScore = if (heartRateZones.averageHR > 0) {
            val totalActiveZones = heartRateZones.fatBurnMinutes + heartRateZones.cardioMinutes + heartRateZones.peakMinutes
            when {
                totalActiveZones >= 30 -> {
                    achievements.add("❤️ Great cardiovascular activity!")
                    25
                }
                totalActiveZones >= 15 -> 20
                totalActiveZones >= 5 -> 15
                else -> 10
            }
        } else {
            recommendations.add("Consider heart rate monitoring during workouts")
            10
        }
        overallScore += heartRateScore
        
        // Determine activity level
        val activityLevel = when {
            overallScore >= 85 -> "Excellent"
            overallScore >= 70 -> "Very Good"
            overallScore >= 55 -> "Good"
            overallScore >= 40 -> "Fair"
            else -> "Needs Improvement"
        }
        
        // Fitness goal progress (based on steps + exercise)
        val fitnessGoalProgress = ((stepsScore + exerciseScore) * 2).coerceIn(0, 100)
        
        // Health trend (simplified)
        val healthTrend = when {
            overallScore >= 70 -> "Improving"
            overallScore >= 50 -> "Stable"
            else -> "Needs Attention"
        }
        
        // Add positive reinforcement
        if (achievements.isEmpty()) {
            achievements.add("Keep up the good work!")
        }
        
        return HealthInsights(
            overallHealthScore = overallScore,
            activityLevel = activityLevel,
            fitnessGoalProgress = fitnessGoalProgress,
            healthTrend = healthTrend,
            recommendations = recommendations.take(3), // Limit to 3 recommendations
            achievements = achievements.take(2) // Limit to 2 achievements
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize permission launcher
        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            // Permission result will be handled by recomposition
        }
        
        setContent {
            HealthPipelineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardWithHealthData()
                }
            }
        }
    }
    
    @Composable
    fun DashboardWithHealthData() {
        val context = LocalContext.current
        var healthConnectStatus by remember { mutableStateOf("Checking...") }
        var isHealthConnectAvailable by remember { mutableStateOf(false) }
        var permissionStatus by remember { mutableStateOf("Checking...") }
        var hasPermissions by remember { mutableStateOf(false) }
        var healthData by remember { mutableStateOf(HealthData()) }
        var isLoadingData by remember { mutableStateOf(false) }
        
        // Cloud sync state
        var cloudSyncStatus by remember { mutableStateOf("Not synced") }
        var isSyncing by remember { mutableStateOf(false) }
        val cloudSyncService = remember { CloudSyncService(context) }
        
        // Check Health Connect availability and permissions
        LaunchedEffect(Unit) {
            try {
                val availabilityStatus = HealthConnectClient.getSdkStatus(context)
                when (availabilityStatus) {
                    HealthConnectClient.SDK_AVAILABLE -> {
                        healthConnectStatus = "Available"
                        isHealthConnectAvailable = true
                        
                        // Check permissions using class-level function
                        val (status, hasPerms) = checkPermissions()
                        permissionStatus = status
                        hasPermissions = hasPerms
                        
                        // Load health data if permissions are granted
                        if (hasPerms) {
                            isLoadingData = true
                            healthData = readHealthData()
                            isLoadingData = false
                        }
                    }
                    HealthConnectClient.SDK_UNAVAILABLE -> {
                        healthConnectStatus = "Not Available"
                        isHealthConnectAvailable = false
                        permissionStatus = "Health Connect not available"
                    }
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                        healthConnectStatus = "Update Required"
                        isHealthConnectAvailable = false
                        permissionStatus = "Update required"
                    }
                    else -> {
                        healthConnectStatus = "Unknown Status"
                        isHealthConnectAvailable = false
                        permissionStatus = "Unknown"
                    }
                }
            } catch (e: Exception) {
                healthConnectStatus = "Error: ${e.message}"
                isHealthConnectAvailable = false
                permissionStatus = "Error checking permissions"
            }
        }
        
        fun requestHealthPermissions() {
            lifecycleScope.launch {
                try {
                    val permissionStrings = permissions.map { it.toString() }.toSet()
                    requestPermissions.launch(permissionStrings)
                    
                    // Recheck permissions after request
                    kotlinx.coroutines.delay(1000) // Wait a bit for permission dialog
                    val (status, hasPerms) = checkPermissions()
                    permissionStatus = status
                    hasPermissions = hasPerms
                    
                    // Load health data if permissions granted
                    if (hasPerms) {
                        isLoadingData = true
                        healthData = readHealthData()
                        isLoadingData = false
                    }
                } catch (e: Exception) {
                    permissionStatus = "Error requesting permissions: ${e.message}"
                }
            }
        }
        
        fun refreshHealthData() {
            lifecycleScope.launch {
                if (hasPermissions) {
                    isLoadingData = true
                    healthData = readHealthData()
                    isLoadingData = false
                }
            }
        }
        
        fun syncToCloud() {
            lifecycleScope.launch {
                if (hasPermissions && healthData.steps > 0) {
                    isSyncing = true
                    cloudSyncStatus = "Syncing..."
                    
                    val result = cloudSyncService.syncHealthData(healthData)
                    
                    if (result.isSuccess) {
                        cloudSyncStatus = "Synced successfully"
                        Log.d("MainActivity", "Health data synced to cloud successfully")
                    } else {
                        cloudSyncStatus = "Sync failed"
                        Log.e("MainActivity", "Cloud sync failed: ${result.exceptionOrNull()?.message}")
                    }
                    
                    isSyncing = false
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Health Pipeline Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Overall Health Score Card (Premium Feature)
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
                    
                    // Progress indicator
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
            
            // Achievements & Recommendations (Premium Feature)
            if (healthData.healthInsights.achievements.isNotEmpty() || healthData.healthInsights.recommendations.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Achievements
                    if (healthData.healthInsights.achievements.isNotEmpty()) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🏆 Achievements",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                healthData.healthInsights.achievements.forEach { achievement ->
                                    Text(
                                        text = achievement,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // Recommendations
                    if (healthData.healthInsights.recommendations.isNotEmpty()) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 Tips",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                healthData.healthInsights.recommendations.take(2).forEach { recommendation ->
                                    Text(
                                        text = recommendation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Health Connect Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Health Connect Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (statusText, statusColor) = when {
                            healthConnectStatus == "Available" && hasPermissions -> "● CONNECTED" to MaterialTheme.colorScheme.primary
                            healthConnectStatus == "Available" && !hasPermissions -> "● PERMISSIONS NEEDED" to MaterialTheme.colorScheme.error
                            healthConnectStatus == "Checking..." -> "● CHECKING..." to MaterialTheme.colorScheme.outline
                            else -> "● NOT AVAILABLE" to MaterialTheme.colorScheme.error
                        }
                        
                        Text(
                            text = statusText,
                            color = statusColor
                        )
                    }
                    
                    if (permissionStatus != "All permissions granted" && permissionStatus != "Checking...") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Status: $permissionStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { requestHealthPermissions() },
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
            
            // Cloud Sync Status Card (New Premium Feature)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cloud Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
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
                            onClick = { syncToCloud() },
                            enabled = hasPermissions && healthData.steps > 0 && !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Sync to Cloud")
                        }
                    }
                }
            }
            
            // Quick Actions Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { refreshHealthData() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasPermissions && !isLoadingData
                    ) {
                        if (isLoadingData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Refresh Health Data")
                    }
                }
            }
            
            // Health Data Cards (Now with real data!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Steps Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${healthData.steps}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Steps Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Heart Rate Summary Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (healthData.heartRateZones.averageHR > 0) "${healthData.heartRateZones.averageHR}" else "--",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Avg Heart Rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Heart Rate Zones Card (Premium Feature)
            if (healthData.heartRateZones.averageHR > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Heart Rate Zones Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Heart Rate Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Resting: ${healthData.heartRateZones.restingHR} BPM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Max: ${healthData.heartRateZones.maxHR} BPM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Avg: ${healthData.heartRateZones.averageHR} BPM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Zone Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Resting Zone
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.heartRateZones.restingMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Resting",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Fat Burn Zone
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.heartRateZones.fatBurnMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Fat Burn",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            
                            // Cardio Zone
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.heartRateZones.cardioMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Cardio",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            
                            // Peak Zone
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.heartRateZones.peakMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Peak",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Distance and Calories Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Distance Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (healthData.distanceKm > 0) String.format("%.1f", healthData.distanceKm) else "0.0",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Distance (km)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Calories Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${healthData.caloriesBurned}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Calories Burned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Sleep Analysis Card (Premium Feature)
            if (healthData.sleepAnalysis.totalSleepHours > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sleep Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            // Sleep Quality Score
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        healthData.sleepAnalysis.sleepQualityScore >= 80 -> MaterialTheme.colorScheme.primaryContainer
                                        healthData.sleepAnalysis.sleepQualityScore >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    }
                                )
                            ) {
                                Text(
                                    text = "${healthData.sleepAnalysis.sleepQualityScore}%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Sleep Duration and Times
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${String.format("%.1f", healthData.sleepAnalysis.totalSleepHours)}h",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total Sleep",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${healthData.sleepAnalysis.sleepEfficiency}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Efficiency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Bed and Wake Times
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bedtime: ${healthData.sleepAnalysis.bedTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Wake: ${healthData.sleepAnalysis.wakeTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Sleep Stages Breakdown
                        Text(
                            text = "Sleep Stages",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Deep Sleep
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.sleepAnalysis.deepSleepMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Deep",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            // Light Sleep
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.sleepAnalysis.lightSleepMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Light",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            
                            // REM Sleep
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.sleepAnalysis.remSleepMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "REM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            
                            // Awake Time
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${healthData.sleepAnalysis.awakeMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Awake",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Simple Sleep Card (fallback)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sleep Last Night",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (healthData.sleepHours > 0) {
                                val hours = healthData.sleepHours.toInt()
                                val minutes = ((healthData.sleepHours - hours) * 60).toInt()
                                "${hours}h ${minutes}m"
                            } else "No data",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            // Exercise Sessions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exercise Sessions Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${healthData.exerciseSessions}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Exercise Sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Active Minutes Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${healthData.activeMinutes}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Active Minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Last Exercise Card
            if (healthData.lastExerciseType != "None") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last Exercise",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = healthData.lastExerciseType,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Status Message
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    Text(
                        text = when {
                            hasPermissions -> "Click 'Refresh Health Data' to update"
                            isHealthConnectAvailable -> "Click 'Grant Permissions' to allow access"
                            else -> "Install Health Connect from Play Store"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
