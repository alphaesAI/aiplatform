package com.healthpipeline

import android.content.Context
import android.util.Log
import com.healthpipeline.data.ApiClient
import com.healthpipeline.data.mappers.HealthDataMapper
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.models.HealthQueueResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudSyncService(private val context: Context) {
    
    private val apiClient = ApiClient(context)
    
    companion object {
        private const val TAG = "CloudSyncService"
    }
    
    /**
     * NEW: Sync health data to queue (PRIMARY METHOD)
     */
    suspend fun syncHealthDataToQueue(
        healthData: HealthData
    ): Result<HealthQueueResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Syncing health data to queue...")
                
                // Generate user ID (in production, use actual user ID)
                val userId = "user_${System.currentTimeMillis() / 86400000}"
                
                // Map Health Connect data to queue format
                val queueRequest = HealthDataMapper.mapToQueueRequest(healthData, userId)
                
                Log.d(TAG, "Queue request: $queueRequest")
                
                // Send to API
                val response = apiClient.healthApiService.queueHealthData(queueRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val queueResponse = response.body()!!
                    Log.d(TAG, "Data queued successfully: ${queueResponse.queueId}")
                    Result.success(queueResponse)
                } else {
                    val error = "Queue failed: ${response.code()} ${response.message()}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Sync error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check queue status
     */
    suspend fun checkQueueStatus(queueId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.healthApiService.getQueueStatus(queueId)
                
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!.status
                    Log.d(TAG, "Queue status: $status")
                    Result.success(status)
                } else {
                    Result.failure(Exception("Status check failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Status check error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Test API connection
     */
    suspend fun testConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing API connection...")
                
                val response = apiClient.healthApiService.healthCheck()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "API connection successful")
                    Result.success(true)
                } else {
                    Log.e(TAG, "API connection failed: ${response.code()}")
                    Result.success(false)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}", e)
                Result.success(false)
            }
        }
    }
}
