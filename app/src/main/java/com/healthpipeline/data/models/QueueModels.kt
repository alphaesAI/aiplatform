package com.healthpipeline.data.models

import com.google.gson.annotations.SerializedName

// This is the object that gets sent to the server.
// It maps exactly to the FastAPI HealthDataRequest model.
data class HealthQueueRequest(
    @SerializedName("pseudo_id") val pseudoId: String,
    @SerializedName("pseudo_id2") val pseudoId2: String? = null,
    @SerializedName("date") val date: String,
    @SerializedName("datetime") val datetime: String? = null,

    // Activity
    @SerializedName("activity_name") val activityName: String? = null,
    @SerializedName("duration_minutes") val durationMinutes: Double? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("avg_hr_bpm") val avgHrBpm: Int? = null,
    @SerializedName("max_hr_bpm") val maxHrBpm: Int? = null,
    @SerializedName("elevation_gain_m") val elevationGainM: Double? = null,
    @SerializedName("distance_meters") val distanceMeters: Double? = null,
    @SerializedName("calories_kcal") val caloriesKcal: Double? = null,
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("speed_mps") val speedMps: Double? = null,

    // Active Zones
    @SerializedName("active_zone_minutes") val activeZoneMinutes: Int? = null,
    @SerializedName("fatburn_active_zone_minutes") val fatburnActiveZoneMinutes: Int? = null,
    @SerializedName("cardio_active_zone_minutes") val cardioActiveZoneMinutes: Int? = null,
    @SerializedName("peak_active_zone_minutes") val peakActiveZoneMinutes: Int? = null,

    // Biometrics
    @SerializedName("age") val age: Int? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("weight_kg") val weightKg: Double? = null,
    @SerializedName("height_cm") val heightCm: Double? = null,

    // Vitals
    @SerializedName("resting_heart_rate") val restingHeartRate: Int? = null,
    @SerializedName("heart_rate_variability") val heartRateVariability: Double? = null,
    @SerializedName("stress_management_score") val stressManagementScore: Int? = null,

    // Sleep
    @SerializedName("sleep_minutes") val sleepMinutes: Int? = null,
    @SerializedName("rem_sleep_minutes") val remSleepMinutes: Int? = null,
    @SerializedName("deep_sleep_minutes") val deepSleepMinutes: Int? = null,
    @SerializedName("awake_minutes") val awakeMinutes: Int? = null,
    @SerializedName("light_sleep_minutes") val lightSleepMinutes: Int? = null,
    @SerializedName("bed_time") val bedTime: String? = null,
    @SerializedName("wake_up_time") val wakeUpTime: String? = null,

    // Sleep Percentages
    @SerializedName("deep_sleep_percent") val deepSleepPercent: Double? = null,
    @SerializedName("rem_sleep_percent") val remSleepPercent: Double? = null,
    @SerializedName("awake_percent") val awakePercent: Double? = null,
    @SerializedName("light_sleep_percent") val lightSleepPercent: Double? = null
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
