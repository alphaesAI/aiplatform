package com.healthpipeline

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthpipeline.data.local.HealthDatabase
import com.healthpipeline.data.models.HealthQueueRequest
import com.healthpipeline.utils.SessionManager

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getUserId()

        if (userId == null) {
            Log.w("SyncWorker", "No user logged in, skipping background sync")
            return Result.failure()
        }

        val database = HealthDatabase.getDatabase(applicationContext)
        val dao = database.healthDataDao()
        val cloudService = CloudSyncService(applicationContext)

        val pendingData = dao.getPendingSyncData()

        if (pendingData.isEmpty()) return Result.success()

        Log.d("SyncWorker", "Found ${pendingData.size} records to sync for user $userId...")

        var allSuccessful = true

        for (entity in pendingData) {
            try {
                // Convert DB Entity to API Request with Real UUID
                val request = HealthQueueRequest(
                    pseudoId = userId, 
                    pseudoId2 = entity.pseudoId2 ?: userId,
                    date = entity.date,
                    datetime = entity.datetime,
                    durationMinutes = entity.duration.toDouble(),
                    activityName = entity.activityName,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    avgHrBpm = entity.averageHeartRate,
                    maxHrBpm = entity.averageHeartRate, // Fallback if max HR is missing in local DB
                    elevationGainM = entity.elevationGain,
                    distanceMeters = entity.distance,
                    caloriesKcal = entity.calories.toDouble(),
                    steps = entity.steps,
                    speedMps = entity.speed,
                    age = entity.age,
                    gender = entity.gender,
                    weightKg = entity.weightKg,
                    heightCm = entity.heightCm,
                    restingHeartRate = entity.restingHeartRate,
                    heartRateVariability = entity.heartRateVariability,
                    stressManagementScore = entity.stressManagementScore,
                    activeZoneMinutes = entity.activeZoneMinutes,
                    fatburnActiveZoneMinutes = entity.fatburnActiveZoneMinutes,
                    cardioActiveZoneMinutes = entity.cardioActiveZoneMinutes,
                    peakActiveZoneMinutes = entity.peakActiveZoneMinutes,
                    bedTime = entity.bedTime,
                    wakeUpTime = entity.wakeUpTime,
                    sleepMinutes = entity.sleepMinutes,
                    awakeMinutes = entity.awakeMinutes,
                    remSleepMinutes = entity.remSleepMinutes,
                    lightSleepMinutes = entity.lightSleepMinutes,
                    deepSleepMinutes = entity.deepSleepMinutes,
                    awakePercent = entity.awakePercent,
                    remSleepPercent = entity.remSleepPercent,
                    lightSleepPercent = entity.lightSleepPercent,
                    deepSleepPercent = entity.deepSleepPercent
                )

                val syncResult = cloudService.syncQueueRequest(request)

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

        return if (allSuccessful) Result.success() else Result.retry()
    }
}
