package com.healthpipeline.data

// Data class for steps information
data class StepsData(
    val steps: Int,
    val date: String,
    val timestamp: String
)

// Data class for heart rate information
data class HeartRateData(
    val averageBpm: Int,
    val maxBpm: Int,
    val date: String,
    val timestamp: String
)

// Data class for sleep information
data class SleepData(
    val durationHours: Double,
    val date: String,
    val timestamp: String
) {
    // Helper function to format sleep duration
    fun getFormattedDuration(): String {
        val hours = durationHours.toInt()
        val minutes = ((durationHours - hours) * 60).toInt()
        return "${hours}h ${minutes}m"
    }
}

// Comprehensive health data summary
data class HealthDataSummary(
    val stepsData: StepsData?,
    val heartRateData: HeartRateData?,
    val sleepData: SleepData?,
    val isHealthConnectAvailable: Boolean,
    val hasPermissions: Boolean,
    val lastUpdated: String
)

// Health Connect status for UI
enum class HealthConnectStatus {
    AVAILABLE,
    NOT_AVAILABLE,
    NOT_INSTALLED,
    PERMISSIONS_NEEDED
}
