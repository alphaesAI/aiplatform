package com.healthpipeline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "health_data_queue")
data class HealthDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val queueId: String = UUID.randomUUID().toString(),
    
    // Core Identity & Session
    val pseudoId2: String,
    val date: String,
    val datetime: String,
    val duration: Long,
    val activityName: String,
    val startTime: String,
    val endTime: String,
    val averageHeartRate: Int,
    val elevationGain: Double,
    val distance: Double,
    val calories: Int,
    val steps: Int,
    val speed: Double,

    // Biometrics
    val age: Int,
    val gender: String,
    val weightKg: Double,
    val heightCm: Double,

    // Vitals
    val restingHeartRate: Int,
    val heartRateVariability: Double,
    val stressManagementScore: Int,

    // Active Zones
    val activeZoneMinutes: Int,
    val fatburnActiveZoneMinutes: Int,
    val cardioActiveZoneMinutes: Int,
    val peakActiveZoneMinutes: Int,

    // Sleep
    val bedTime: String,
    val wakeUpTime: String,
    val sleepMinutes: Int,
    val awakeMinutes: Int,
    val remSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val deepSleepMinutes: Int,
    val awakePercent: Double,
    val remSleepPercent: Double,
    val lightSleepPercent: Double,
    val deepSleepPercent: Double,

    // Sync Tracking fields
    val status: String = "pending",
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
