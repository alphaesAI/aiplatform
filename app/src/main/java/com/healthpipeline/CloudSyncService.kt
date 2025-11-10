package com.healthpipeline

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudSyncService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // Network configuration - automatically detects emulator vs physical device
    private val baseUrl = if (android.os.Build.FINGERPRINT.contains("generic")) {
        "http://10.0.2.2:8000"  // Android emulator
    } else {
        "http://172.17.61.30:8000"  // Physical device - your computer's IP
    }
    
    companion object {
        private const val TAG = "CloudSyncService"
    }
    
    data class SyncResponse(
        val status: String,
        val message: String,
        val user_id: String,
        val timestamp: String
    )
    
    data class HealthHistoryResponse(
        val status: String,
        val user_id: String,
        val days: Int,
        val records_count: Int,
        val history: List<HealthRecord>,
        val timestamp: String
    )
    
    data class HealthRecord(
        val user_id: String,
        val date: String,
        val timestamp: String,
        val health_metrics: HealthMetrics,
        val advanced_analytics: AdvancedAnalytics
    )
    
    data class HealthMetrics(
        val steps: Int,
        val heart_rate_avg: Int,
        val sleep_hours: Double,
        val calories: Int,
        val distance_km: Double,
        val exercise_sessions: Int,
        val active_minutes: Int
    )
    
    data class AdvancedAnalytics(
        val health_score: Int,
        val activity_level: String,
        val heart_rate_zones: Map<String, Any>,
        val sleep_analysis: Map<String, Any>
    )
    
    suspend fun syncHealthData(healthData: MainActivity.HealthData): Result<SyncResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting health data sync to cloud...")
                
                // Prepare sync data - match proxy server format
                val syncData = mapOf(
                    "userId" to "android_user_${System.currentTimeMillis() / 86400000}",
                    "steps" to healthData.steps,
                    "heartRate" to healthData.heartRateZones.averageHR,
                    "sleepDuration" to healthData.sleepAnalysis.totalSleepHours.toInt(),
                    "distance" to healthData.distanceKm.toFloat(),
                    "calories" to healthData.caloriesBurned
                )
                
                val json = gson.toJson(syncData)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url("$baseUrl/sync")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val syncResponse = gson.fromJson(responseBody, SyncResponse::class.java)
                    Log.d(TAG, "Health data sync successful: ${syncResponse.message}")
                    Result.success(syncResponse)
                } else {
                    val error = "Sync failed: ${response.code} ${response.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "Network error during sync: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sync: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun getHealthHistory(userId: String, days: Int = 7): Result<HealthHistoryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching health history for $userId...")
                
                val request = Request.Builder()
                    .url("$baseUrl/api/health/history/$userId?days=$days")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val historyResponse = gson.fromJson(responseBody, HealthHistoryResponse::class.java)
                    Log.d(TAG, "Health history retrieved: ${historyResponse.records_count} records")
                    Result.success(historyResponse)
                } else {
                    val error = "History fetch failed: ${response.code} ${response.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
                
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching history: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching history: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun testConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing cloud connection...")
                
                val request = Request.Builder()
                    .url("$baseUrl/health")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Cloud connection successful")
                    Result.success(true)
                } else {
                    Log.e(TAG, "Cloud connection failed: ${response.code}")
                    Result.success(false)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Cloud connection error: ${e.message}")
                Result.success(false)
            }
        }
    }
}
