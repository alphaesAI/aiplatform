package com.healthpipeline.data.models

data class HealthData(
    val steps: Int = 0,
    val heartRate: Int = 0,
    val sleepHours: Double = 0.0,
    val distanceKm: Double = 0.0,
    val caloriesBurned: Int = 0,
    val exerciseSessions: Int = 0,
    val activeMinutes: Int = 0,
    val lastExerciseType: String = "None",
    val heartRateZones: HeartRateZones = HeartRateZones(),
    val sleepAnalysis: SleepAnalysis = SleepAnalysis(),
    val healthInsights: HealthInsights = HealthInsights()
)

data class HeartRateZones(
    val restingMinutes: Int = 0,
    val fatBurnMinutes: Int = 0,
    val cardioMinutes: Int = 0,
    val peakMinutes: Int = 0,
    val restingHR: Int = 0,
    val maxHR: Int = 0,
    val averageHR: Int = 0
)

data class SleepAnalysis(
    val totalSleepHours: Double = 0.0,
    val deepSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val remSleepMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val sleepEfficiency: Int = 0,
    val sleepQualityScore: Int = 0,
    val bedTime: String = "No data",
    val wakeTime: String = "No data"
)

data class HealthInsights(
    val overallHealthScore: Int = 0,
    val activityLevel: String = "Unknown",
    val fitnessGoalProgress: Int = 0,
    val healthTrend: String = "Stable",
    val recommendations: List<String> = emptyList(),
    val achievements: List<String> = emptyList()
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}