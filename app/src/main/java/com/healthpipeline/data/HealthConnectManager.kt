package com.healthpipeline.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthpipeline.data.models.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun checkPermissions(): Pair<String, Boolean> {
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
    suspend fun readHealthData(): HealthData {
        return try {
            val steps = readTodaySteps()
            Log.d("HealthData", "Steps extracted: $steps")
            val heartRate = readTodayHeartRate()
            Log.d("HealthData", "HeartRate extracted: $heartRate")
            val sleep = readLastNightSleep()
            val distance = readTodayDistance()
            Log.d("HealthData", "Distance extracted: $distance")
            val calories = readTodayCalories()
            Log.d("HealthData", "Calories extracted: $calories")
            val (exerciseSessions, activeMinutes, lastExercise) = readTodayExercise()
            val heartRateZones = readHeartRateZones()
            val sleepAnalysis = readSleepAnalysis()
            
            // Estimate missing data from available data
            val finalCalories = if (calories == 0 && steps > 0) {
                (steps * 0.04).toInt() // Estimate: 0.04 cal per step
            } else calories
            
            val finalActiveMinutes = if (activeMinutes == 0 && distance > 0) {
                (distance * 12).toInt() // Estimate: 12 min per km
            } else activeMinutes
            
            val finalHeartRate = if (heartRate == 0 && steps > 0) {
                75 // Average resting HR
            } else heartRate
            
            Log.d("HealthData", "Final values - Calories: $finalCalories, Active: $finalActiveMinutes, HR: $finalHeartRate")
            
            // Calculate health insights based on all data
            val healthInsights = calculateHealthInsights(steps, heartRateZones, sleepAnalysis, exerciseSessions, finalActiveMinutes, finalCalories)
            
            HealthData(steps, finalHeartRate, sleep, distance, finalCalories, exerciseSessions, finalActiveMinutes, lastExercise, heartRateZones, sleepAnalysis, healthInsights)
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
}