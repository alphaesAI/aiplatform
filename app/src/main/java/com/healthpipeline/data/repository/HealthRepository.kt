package com.healthpipeline.data.repository

import android.util.Log
import com.healthpipeline.CloudSyncService
import com.healthpipeline.data.HealthConnectManager
import com.healthpipeline.data.local.HealthDataDao
import com.healthpipeline.data.local.HealthDataEntity
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.models.HealthQueueResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthRepository(
    private val healthConnectManager: HealthConnectManager,
    private val cloudSyncService: CloudSyncService,
    private val healthDataDao: HealthDataDao // NEW: Our local SQLite Database!
) {
    suspend fun checkPermissions(): Pair<String, Boolean> {
        return healthConnectManager.checkPermissions()
    }

    suspend fun getTodayHealthData(): HealthData {
        return healthConnectManager.readHealthData()
    }
    

    
    suspend fun syncHealthData(data: HealthData): Result<HealthQueueResponse> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        val userId = "user_${System.currentTimeMillis() / 86400000}"

        val localEntity = HealthDataEntity(
            pseudoId = userId,
            date = todayDate,
            steps = data.steps,
            caloriesKcal = data.caloriesBurned.toFloat(),
            distanceMeters = (data.distanceKm * 1000).toFloat(),
            avgHrBpm = data.heartRateZones.averageHR,
            status = "pending" 
        )

        try {
            healthDataDao.insertHealthData(localEntity)
            Log.d("HealthRepository", "Saved to local Room DB with status: pending")

            val result = cloudSyncService.syncHealthDataToQueue(data)

            if (result.isSuccess) {
                healthDataDao.updateStatus(localEntity.queueId, "synced")
                Log.d("HealthRepository", "Cloud sync successful. Local DB updated to: synced")
            } else {
                healthDataDao.updateStatus(localEntity.queueId, "failed")
                Log.d("HealthRepository", "Cloud sync failed. Local DB updated to: failed")
            }

            return result

        } catch (e: Exception) {
            healthDataDao.updateStatus(localEntity.queueId, "failed")
            Log.e("HealthRepository", "No internet or server down. Data safely waiting in local DB.")
            return Result.failure(e)
        }
    }
}