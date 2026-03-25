package com.healthpipeline

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthpipeline.data.local.HealthDatabase
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.models.HeartRateZones

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = HealthDatabase.getDatabase(applicationContext)
        val dao = database.healthDataDao()
        val cloudService = CloudSyncService(applicationContext)

        // 1. Find all data that failed to sync earlier
        val pendingData = dao.getPendingSyncData()
        
        if (pendingData.isEmpty()) return Result.success()

        Log.d("SyncWorker", "Found ${pendingData.size} records to sync...")

        var allSuccessful = true

        for (entity in pendingData) {
            try {
                // 2. Convert the Database Entity back into the HealthData model for the API
                val healthData = HealthData(
                    steps = entity.steps ?: 0,
                    caloriesBurned = entity.caloriesKcal?.toInt() ?: 0,
                    distanceKm = (entity.distanceMeters ?: 0f) / 1000f.toDouble(),
                    heartRateZones = HeartRateZones(averageHR = entity.avgHrBpm ?: 0)
                )

                // 3. Try to sync to FastAPI
                val syncResult = cloudService.syncHealthDataToQueue(healthData)

                if (syncResult.isSuccess) {
                    dao.updateStatus(entity.queueId, "synced")
                    Log.d("SyncWorker", "Successfully synced record: ${entity.queueId}")
                } else {
                    allSuccessful = false
                }
            } catch (e: Exception) {
                allSuccessful = false
                Log.e("SyncWorker", "Error syncing record ${entity.queueId}: ${e.message}")
            }
        }

        // If anything failed, tell WorkManager to try again later when internet is better
        return if (allSuccessful) Result.success() else Result.retry()
    }
}