package com.healthpipeline.data.mappers

import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.models.HealthQueueRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object HealthDataMapper {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun mapToQueueRequests(
        healthData: HealthData,
        userId: String
    ): List<HealthQueueRequest> {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val todayDateStr = today.format(dateFormatter)
        val datetimeStr = now.format(dateTimeFormatter)
        
        // If there are no sessions, we create a "Summary" request
        if (healthData.exerciseSessionsList.isEmpty()) {
            return listOf(createSummaryRequest(healthData, userId, todayDateStr, datetimeStr))
        }

        // Map each specific session
        return healthData.exerciseSessionsList.map { session ->
            HealthQueueRequest(
                pseudoId = userId,
                pseudoId2 = userId,
                date = todayDateStr,
                datetime = datetimeStr,
                
                activityName = session.activityName,
                durationMinutes = session.duration.toDouble() / 60.0,
                startTime = session.startTime,
                endTime = session.endTime,
                avgHrBpm = session.averageHeartRate,
                maxHrBpm = session.averageHeartRate + 20,
                elevationGainM = session.elevationGain,
                distanceMeters = session.distance,
                caloriesKcal = session.calories.toDouble(),
                steps = session.steps,
                speedMps = session.speed,
                
                activeZoneMinutes = session.activeZoneMinutes.takeIf { it > 0 } ?: healthData.totalActiveMinutes,
                fatburnActiveZoneMinutes = session.fatburnActiveZoneMinutes,
                cardioActiveZoneMinutes = session.cardioActiveZoneMinutes,
                peakActiveZoneMinutes = session.peakActiveZoneMinutes,
                
                age = healthData.biometrics.age,
                gender = healthData.biometrics.gender,
                weightKg = healthData.biometrics.weightKg,
                heightCm = healthData.biometrics.heightCm,
                
                restingHeartRate = healthData.vitals.restingHeartRate,
                heartRateVariability = healthData.vitals.heartRateVariability,
                stressManagementScore = healthData.vitals.stressManagementScore,
                
                bedTime = healthData.sleepAnalysis.bedTime,
                wakeUpTime = healthData.sleepAnalysis.wakeUpTime,
                sleepMinutes = healthData.sleepAnalysis.sleepMinutes,
                awakeMinutes = healthData.sleepAnalysis.awakeMinutes,
                remSleepMinutes = healthData.sleepAnalysis.remSleepMinutes,
                lightSleepMinutes = healthData.sleepAnalysis.lightSleepMinutes,
                deepSleepMinutes = healthData.sleepAnalysis.deepSleepMinutes,
                awakePercent = healthData.sleepAnalysis.awakePercent,
                remSleepPercent = healthData.sleepAnalysis.remSleepPercent,
                lightSleepPercent = healthData.sleepAnalysis.lightSleepPercent,
                deepSleepPercent = healthData.sleepAnalysis.deepSleepPercent
            )
        }
    }

    private fun createSummaryRequest(
        healthData: HealthData, 
        userId: String, 
        todayDateStr: String, 
        datetimeStr: String
    ): HealthQueueRequest {
        return HealthQueueRequest(
            pseudoId = userId,
            pseudoId2 = userId,
            date = todayDateStr,
            datetime = datetimeStr,
            
            activityName = "Daily Summary",
            durationMinutes = healthData.totalActiveMinutes.toDouble(),
            startTime = "00:00",
            endTime = "23:59",
            avgHrBpm = healthData.vitals.restingHeartRate,
            distanceMeters = healthData.totalDistanceKm * 1000,
            caloriesKcal = healthData.totalCaloriesBurned.toDouble(),
            steps = healthData.totalSteps,
            
            age = healthData.biometrics.age,
            gender = healthData.biometrics.gender,
            weightKg = healthData.biometrics.weightKg,
            heightCm = healthData.biometrics.heightCm,
            
            restingHeartRate = healthData.vitals.restingHeartRate,
            heartRateVariability = healthData.vitals.heartRateVariability,
            stressManagementScore = healthData.vitals.stressManagementScore,
            
            activeZoneMinutes = healthData.totalActiveMinutes,
            
            bedTime = healthData.sleepAnalysis.bedTime,
            wakeUpTime = healthData.sleepAnalysis.wakeUpTime,
            sleepMinutes = healthData.sleepAnalysis.sleepMinutes,
            awakeMinutes = healthData.sleepAnalysis.awakeMinutes,
            remSleepMinutes = healthData.sleepAnalysis.remSleepMinutes,
            lightSleepMinutes = healthData.sleepAnalysis.lightSleepMinutes,
            deepSleepMinutes = healthData.sleepAnalysis.deepSleepMinutes,
            awakePercent = healthData.sleepAnalysis.awakePercent,
            remSleepPercent = healthData.sleepAnalysis.remSleepPercent,
            lightSleepPercent = healthData.sleepAnalysis.lightSleepPercent,
            deepSleepPercent = healthData.sleepAnalysis.deepSleepPercent
        )
    }
}
