package com.healthpipeline.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    // Health Connect permissions
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )
    
    // Check if Health Connect is available
    suspend fun isAvailable(): Boolean {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> true
            else -> false
        }
    }
    
    // Check if permissions are granted
    suspend fun hasAllPermissions(): Boolean {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        return permissions.all { it in grantedPermissions }
    }
    
    // Get steps data for today
    suspend fun getTodaySteps(): StepsData? {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.truncatedTo(ChronoUnit.DAYS).toInstant()
            val endOfDay = today.plusDays(1).truncatedTo(ChronoUnit.DAYS).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            val totalSteps = response.records.sumOf { it.count }
            
            StepsData(
                steps = totalSteps.toInt(),
                date = today.toLocalDate().toString(),
                timestamp = Instant.now().toString()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Get heart rate data for today
    suspend fun getTodayHeartRate(): HeartRateData? {
        return try {
            val today = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val startOfDay = today.truncatedTo(ChronoUnit.DAYS).toInstant()
            val endOfDay = today.plusDays(1).truncatedTo(ChronoUnit.DAYS).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            
            val response = healthConnectClient.readRecords(request)
            val heartRates = response.records.flatMap { it.samples }
            
            if (heartRates.isNotEmpty()) {
                val avgHeartRate = heartRates.map { it.beatsPerMinute }.average()
                val maxHeartRate = heartRates.maxOf { it.beatsPerMinute }
                
                HeartRateData(
                    averageBpm = avgHeartRate.toInt(),
                    maxBpm = maxHeartRate.toInt(),
                    date = today.toLocalDate().toString(),
                    timestamp = Instant.now().toString()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // Get sleep data for last night
    suspend fun getLastNightSleep(): SleepData? {
        return try {
            val now = Instant.now()
            val yesterday = now.minus(1, ChronoUnit.DAYS)
            
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(yesterday, now)
            )
            
            val response = healthConnectClient.readRecords(request)
            val sleepSessions = response.records
            
            if (sleepSessions.isNotEmpty()) {
                val totalDuration = sleepSessions.sumOf { 
                    it.endTime.epochSecond - it.startTime.epochSecond 
                }
                val hours = totalDuration / 3600.0
                
                SleepData(
                    durationHours = hours,
                    date = LocalDateTime.ofInstant(yesterday, ZoneId.systemDefault()).toLocalDate().toString(),
                    timestamp = Instant.now().toString()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // Get comprehensive health summary
    suspend fun getHealthSummary(): HealthDataSummary {
        val steps = getTodaySteps()
        val heartRate = getTodayHeartRate()
        val sleep = getLastNightSleep()
        
        return HealthDataSummary(
            stepsData = steps,
            heartRateData = heartRate,
            sleepData = sleep,
            isHealthConnectAvailable = isAvailable(),
            hasPermissions = hasAllPermissions(),
            lastUpdated = Instant.now().toString()
        )
    }
}
