package com.healthpipeline.data

import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import com.healthpipeline.data.models.HealthQueueRequest
import com.healthpipeline.data.models.HealthQueueResponse
import com.healthpipeline.data.models.QueueStatusResponse

// Legacy API Request Models (keep for backward compatibility)
data class HealthDataRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("data_type") val dataType: String,
    @SerializedName("data") val data: List<Any>,
    @SerializedName("metadata") val metadata: Map<String, Any>? = null
)

data class SearchRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("query") val query: String,
    @SerializedName("data_types") val dataTypes: List<String>? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("limit") val limit: Int = 10
)

// API Response Models
data class ApiResponse(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("estimated_processing_time_seconds") val estimatedProcessingTime: Double? = null
)

data class ProcessingStatusResponse(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("progress") val progress: Double,
    @SerializedName("records_total") val recordsTotal: Int,
    @SerializedName("records_processed") val recordsProcessed: Int,
    @SerializedName("current_stage") val currentStage: String?,
    @SerializedName("error_message") val errorMessage: String?
)

data class ProcessingResultResponse(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("total_records") val totalRecords: Int,
    @SerializedName("processed_records") val processedRecords: Int,
    @SerializedName("failed_records") val failedRecords: Int,
    @SerializedName("embeddings_created") val embeddingsCreated: Int,
    @SerializedName("storage_size_mb") val storageSizeMb: Double,
    @SerializedName("processing_time_seconds") val processingTimeSeconds: Double
)

data class SearchResult(
    @SerializedName("record_id") val recordId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("data_type") val dataType: String,
    @SerializedName("original_data") val originalData: Map<String, Any>,
    @SerializedName("ai_text") val aiText: String,
    @SerializedName("similarity_score") val similarityScore: Double,
    @SerializedName("timestamp") val timestamp: String
)

data class SearchResponse(
    @SerializedName("query") val query: String,
    @SerializedName("total_results") val totalResults: Int,
    @SerializedName("results") val results: List<SearchResult>,
    @SerializedName("processing_time_ms") val processingTimeMs: Double
)

data class HealthSummaryResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("total_records") val totalRecords: Int,
    @SerializedName("records_by_type") val recordsByType: Map<String, Int>,
    @SerializedName("last_updated") val lastUpdated: String
)

// API Service Interface
interface HealthApiService {
    
    // NEW: Queue-based endpoints (PRIMARY)
    @POST("api/health/queue")
    suspend fun queueHealthData(
        @Body request: HealthQueueRequest
    ): Response<HealthQueueResponse>
    
    @GET("api/health/status/{queueId}")
    suspend fun getQueueStatus(
        @Path("queueId") queueId: String
    ): Response<QueueStatusResponse>
    
    // Legacy endpoints (keep for backward compatibility)
    @POST("api/health/data")
    suspend fun sendHealthData(@Body request: HealthDataRequest): Response<ApiResponse>
    
    @GET("api/health/status/{jobId}")
    suspend fun getProcessingStatus(@Path("jobId") jobId: String): Response<ProcessingStatusResponse>
    
    @GET("api/health/results/{jobId}")
    suspend fun getProcessingResults(@Path("jobId") jobId: String): Response<ProcessingResultResponse>
    
    @POST("api/health/search")
    suspend fun searchHealthData(@Body request: SearchRequest): Response<SearchResponse>
    
    @GET("api/health/summary/{userId}")
    suspend fun getHealthSummary(
        @Path("userId") userId: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<HealthSummaryResponse>
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}
