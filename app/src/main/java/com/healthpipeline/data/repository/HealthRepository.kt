package com.healthpipeline.data.repository

import android.util.Log
import com.healthpipeline.CloudSyncService
import com.healthpipeline.data.HealthConnectManager
import com.healthpipeline.data.local.HealthDataDao
import com.healthpipeline.data.local.HealthDataEntity
import com.healthpipeline.data.mappers.HealthDataMapper
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.VitalRecord

class HealthRepository(
    private val healthConnectManager: HealthConnectManager,
    private val cloudSyncService: CloudSyncService,
    private val healthDataDao: HealthDataDao
) {
    suspend fun checkPermissions(): Pair<String, Boolean> {
        return healthConnectManager.checkPermissions()
    }

    suspend fun getTodayHealthData(): HealthData {
        return healthConnectManager.readHealthData()
    }
    
    suspend fun syncHealthData(data: HealthData, userId: String): Result<VitalRecord> {
        // Map UI Data to Queue Requests so we have a flat structure
        val queueRequests = HealthDataMapper.mapToQueueRequests(data, userId)
        
        val entities = queueRequests.map { req ->
            HealthDataEntity(
                pseudoId2 = req.pseudoId2 ?: userId,
                date = req.date,
                datetime = req.datetime ?: "",
                duration = req.durationMinutes?.toLong() ?: 0L,
                activityName = req.activityName ?: "Activity",
                startTime = req.startTime ?: "--:--",
                endTime = req.endTime ?: "--:--",
                averageHeartRate = req.avgHrBpm ?: 0,
                elevationGain = req.elevationGainM ?: 0.0,
                distance = req.distanceMeters ?: 0.0,
                calories = req.caloriesKcal?.toInt() ?: 0,
                steps = req.steps ?: 0,
                speed = req.speedMps ?: 0.0,
                
                age = req.age ?: 0,
                gender = req.gender ?: "Not Specified",
                weightKg = req.weightKg ?: 0.0,
                heightCm = req.heightCm ?: 0.0,
                
                restingHeartRate = req.restingHeartRate ?: 0,
                heartRateVariability = req.heartRateVariability ?: 0.0,
                stressManagementScore = req.stressManagementScore ?: 0,
                
                activeZoneMinutes = req.activeZoneMinutes ?: 0,
                fatburnActiveZoneMinutes = req.fatburnActiveZoneMinutes ?: 0,
                cardioActiveZoneMinutes = req.cardioActiveZoneMinutes ?: 0,
                peakActiveZoneMinutes = req.peakActiveZoneMinutes ?: 0,
                
                bedTime = req.bedTime ?: "--:--",
                wakeUpTime = req.wakeUpTime ?: "--:--",
                sleepMinutes = req.sleepMinutes ?: 0,
                awakeMinutes = req.awakeMinutes ?: 0,
                remSleepMinutes = req.remSleepMinutes ?: 0,
                lightSleepMinutes = req.lightSleepMinutes ?: 0,
                deepSleepMinutes = req.deepSleepMinutes ?: 0,
                awakePercent = req.awakePercent ?: 0.0,
                remSleepPercent = req.remSleepPercent ?: 0.0,
                lightSleepPercent = req.lightSleepPercent ?: 0.0,
                deepSleepPercent = req.deepSleepPercent ?: 0.0,
                
                status = "pending"
            )
        }

        try {
            // Save to Local DB First (Offline-first approach)
            for (entity in entities) {
                healthDataDao.insertHealthData(entity)
            }
            
            // 🔥 Sync to FHIR cloud with Real User ID (NEW METHOD)
            val result = cloudSyncService.syncHealthDataToVitals(data, userId)

            if (result.isSuccess) {
                for (entity in entities) {
                    healthDataDao.updateStatus(entity.queueId, "synced")
                }
            } else {
                for (entity in entities) {
                    healthDataDao.updateStatus(entity.queueId, "failed")
                }
            }

            return result

        } catch (e: Exception) {
            for (entity in entities) {
                healthDataDao.updateStatus(entity.queueId, "failed")
            }
            return Result.failure(e)
        }
    }
}
