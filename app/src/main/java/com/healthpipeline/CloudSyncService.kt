package com.healthpipeline

import android.content.Context
import android.util.Log
import com.healthpipeline.data.ApiClient
import com.healthpipeline.data.VitalsRequest
import com.healthpipeline.data.VitalRecord
import com.healthpipeline.data.mappers.HealthDataMapper
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.models.HealthQueueResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CloudSyncService(private val context: Context) {
    
    private val apiClient = ApiClient(context)
    
    companion object {
        private const val TAG = "CloudSyncService"
    }
    
    /**
     * 🔥 NEW: Sync health data to FHIR vitals endpoint
     * Uses POST /api/v1/vitals/
     */
    suspend fun syncHealthDataToVitals(
        healthData: HealthData,
        userId: String
    ): Result<VitalRecord> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔥🔥🔥 USING NEW FHIR SYNC METHOD for user: $userId")
                
                // Convert HealthData to FHIR VitalsRequest (ALL fields)
                val now = LocalDateTime.now()
                
                // Get first exercise session if available
                val firstExercise = healthData.exerciseSessionsList.firstOrNull()
                
                val vitalsRequest = VitalsRequest(
                    // Core Activity Metrics
                    steps = healthData.totalSteps,
                    caloriesKcal = healthData.totalCaloriesBurned.toDouble(),
                    distanceMeters = healthData.totalDistanceKm * 1000,
                    totalActiveMinutes = healthData.totalActiveMinutes,
                    
                    // Exercise Data
                    activityName = firstExercise?.activityName,
                    exerciseDurationMinutes = firstExercise?.duration?.toDouble()?.div(60.0),
                    activeZoneMinutes = firstExercise?.activeZoneMinutes ?: healthData.totalActiveMinutes,
                    fatburnActiveZoneMinutes = firstExercise?.fatburnActiveZoneMinutes,
                    cardioActiveZoneMinutes = firstExercise?.cardioActiveZoneMinutes,
                    peakActiveZoneMinutes = firstExercise?.peakActiveZoneMinutes,
                    
                    // Vitals
                    restingHeartRate = healthData.vitals.restingHeartRate,
                    heartRate = healthData.vitals.restingHeartRate, // Same as resting for now
                    heartRateVariability = healthData.vitals.heartRateVariability,
                    stressManagementScore = healthData.vitals.stressManagementScore,
                    // bloodPressure - not available from Health Connect
                    
                    // Sleep Data
                    sleepMinutes = healthData.sleepAnalysis.sleepMinutes,
                    remSleepMinutes = healthData.sleepAnalysis.remSleepMinutes,
                    deepSleepMinutes = healthData.sleepAnalysis.deepSleepMinutes,
                    lightSleepMinutes = healthData.sleepAnalysis.lightSleepMinutes,
                    awakeMinutes = healthData.sleepAnalysis.awakeMinutes,
                    bedTime = healthData.sleepAnalysis.bedTime,
                    wakeUpTime = healthData.sleepAnalysis.wakeUpTime,
                    deepSleepPercent = healthData.sleepAnalysis.deepSleepPercent,
                    remSleepPercent = healthData.sleepAnalysis.remSleepPercent,
                    lightSleepPercent = healthData.sleepAnalysis.lightSleepPercent,
                    awakePercent = healthData.sleepAnalysis.awakePercent,
                    
                    // Biometrics
                    weightKg = healthData.biometrics.weightKg,
                    heightCm = healthData.biometrics.heightCm,
                    // age, gender - not available from Health Connect
                    
                    // Metadata (server auto-sets date)
                    recordedAt = now.format(DateTimeFormatter.ISO_DATE_TIME)
                )
                
                val response = apiClient.healthApiService.createVitals(vitalsRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    Log.d(TAG, "✅ Vitals synced successfully: ${result.id}")
                    Result.success(result)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val error = "Vitals sync failed: ${response.code()} - $errorBody"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Vitals sync error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * � NEW: Get user's vitals from FHIR
     * Uses GET /api/v1/vitals/me
     */
    suspend fun getMyVitals(): Result<List<VitalRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔥 Fetching my vitals from FHIR")
                
                val response = apiClient.healthApiService.getMyVitals()
                
                if (response.isSuccessful && response.body() != null) {
                    val vitals = response.body()!!
                    Log.d(TAG, "✅ Got ${vitals.size} vitals records")
                    Result.success(vitals)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val error = "Get vitals failed: ${response.code()} - $errorBody"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Get vitals error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * ⚠️ DEPRECATED: Old queue method - use syncHealthDataToVitals instead
     */
    @Deprecated("Use syncHealthDataToVitals() instead")
    suspend fun syncHealthDataToQueue(
        healthData: HealthData,
        userId: String
    ): Result<HealthQueueResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.w(TAG, "⚠️⚠️⚠️ USING OLD DEPRECATED QUEUE METHOD - THIS WILL FAIL!")
                
                val queueRequests = HealthDataMapper.mapToQueueRequests(healthData, userId)
                
                var lastResponse: HealthQueueResponse? = null
                
                for (request in queueRequests) {
                    val response = apiClient.healthApiService.queueHealthData(request)
                    
                    if (response.isSuccessful && response.body() != null) {
                        lastResponse = response.body()!!
                        Log.d(TAG, "Data queued successfully for $userId (Activity: ${request.activityName})")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val error = "Queue failed for ${request.activityName}: ${response.code()} - $errorBody"
                        Log.e(TAG, error)
                        return@withContext Result.failure(Exception(error))
                    }
                }
                
                if (lastResponse != null) {
                    Result.success(lastResponse)
                } else {
                    Result.failure(Exception("No data to sync"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun syncQueueRequest(request: com.healthpipeline.data.models.HealthQueueRequest): Result<HealthQueueResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.healthApiService.queueHealthData(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val error = "Queue failed: ${response.code()} - $errorBody"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun testConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.healthApiService.healthCheck()
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Result.success(false)
            }
        }
    }
}
