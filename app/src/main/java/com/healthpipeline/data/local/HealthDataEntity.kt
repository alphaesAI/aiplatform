package com.healthpipeline.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "health_data_queue")
data class HealthDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val queueId: String = UUID.randomUUID().toString(),
    val pseudoId: String,
    val date: String,
    val durationMinutes: Float? = null,
    val activityName: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val avgHrBpm: Int? = null,
    val maxHrBpm: Int? = null,
    val elevationGainM: Float? = null,
    val distanceMeters: Float? = null,
    val caloriesKcal: Float? = null,
    val steps: Int? = null,
    val activeZoneMinutes: Int? = null,
    val speedMps: Float? = null,
    
    // Sync tracking fields
    val status: String = "pending",
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)