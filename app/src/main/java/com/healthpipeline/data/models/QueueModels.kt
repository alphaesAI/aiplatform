package com.healthpipeline.data.models

import com.google.gson.annotations.SerializedName

data class HealthQueueRequest(
    @SerializedName("pseudo_id") val pseudoId: String,
    @SerializedName("date") val date: String,
    @SerializedName("duration_minutes") val durationMinutes: Float? = null,
    @SerializedName("activity_name") val activityName: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("avg_hr_bpm") val avgHrBpm: Int? = null,
    @SerializedName("max_hr_bpm") val maxHrBpm: Int? = null,
    @SerializedName("elevation_gain_m") val elevationGainM: Float? = null,
    @SerializedName("distance_meters") val distanceMeters: Float? = null,
    @SerializedName("calories_kcal") val caloriesKcal: Float? = null,
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("active_zone_minutes") val activeZoneMinutes: Int? = null,
    @SerializedName("speed_mps") val speedMps: Float? = null
)

data class HealthQueueResponse(
    @SerializedName("queue_id") val queueId: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("created_at") val createdAt: String
)

data class QueueStatusResponse(
    @SerializedName("queue_id") val queueId: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("error_message") val errorMessage: String?
)
