package com.healthpipeline.data.mappers

import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.models.HealthQueueRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object HealthDataMapper {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun mapToQueueRequest(
        healthData: HealthData,
        userId: String
    ): HealthQueueRequest {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        
        // Calculate speed if we have distance and time
        val speedMps = if (healthData.activeMinutes > 0 && healthData.distanceKm > 0) {
            (healthData.distanceKm * 1000 / (healthData.activeMinutes * 60)).toFloat()
        } else null
        
        return HealthQueueRequest(
            pseudoId = userId,
            date = today.format(dateFormatter),
            durationMinutes = healthData.activeMinutes.toFloat(),
            activityName = healthData.lastExerciseType.takeIf { it != "None" },
            startTime = now.minusMinutes(healthData.activeMinutes.toLong()).format(dateTimeFormatter),
            endTime = now.format(dateTimeFormatter),
            avgHrBpm = healthData.heartRateZones.averageHR.takeIf { it > 0 },
            maxHrBpm = healthData.heartRateZones.maxHR.takeIf { it > 0 },
            elevationGainM = null, // Not available from Health Connect
            distanceMeters = (healthData.distanceKm * 1000).toFloat(),
            caloriesKcal = healthData.caloriesBurned.toFloat(),
            steps = healthData.steps,
            activeZoneMinutes = healthData.activeMinutes,
            speedMps = speedMps
        )
    }
}
