package com.healthpipeline.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface HealthDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthData(data: HealthDataEntity)

    @Query("SELECT * FROM health_data_queue WHERE status = 'pending' OR status = 'failed' ORDER BY createdAt ASC")
    suspend fun getPendingSyncData(): List<HealthDataEntity>

    @Update
    suspend fun updateHealthData(data: HealthDataEntity)

    @Query("UPDATE health_data_queue SET status = :newStatus, updatedAt = :timestamp WHERE queueId = :targetQueueId")
    suspend fun updateStatus(targetQueueId: String, newStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM health_data_queue WHERE status = 'synced' AND createdAt < :olderThanTimestamp")
    suspend fun deleteOldSyncedData(olderThanTimestamp: Long)

}