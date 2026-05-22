package com.healthpipeline.data.models

// 1. Session & Activity Metrics
data class ExerciseSessionDetails(
    val id: String = "",
    val pseudoId2: String = "",
    val date: String = "",
    val datetime: String = "",
    val activityName: String = "Exercise",
    val startTime: String = "--:--",
    val endTime: String = "--:--",
    val duration: Long = 0,
    val averageHeartRate: Int = 0,
    val elevationGain: Double = 0.0,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val steps: Int = 0,
    val speed: Double = 0.0,
    
    // Active Zones
    val activeZoneMinutes: Int = 0,
    val fatburnActiveZoneMinutes: Int = 0,
    val cardioActiveZoneMinutes: Int = 0,
    val peakActiveZoneMinutes: Int = 0
)

// 2. Profile & Biometrics
data class UserBiometrics(
    val age: Int? = null,
    val gender: String? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null
)

// 3. Vitals & Stress
data class Vitals(
    val restingHeartRate: Int? = null,
    val heartRateVariability: Double? = null,
    val stressManagementScore: Int? = null
)

// 4. Detailed Sleep Analysis
data class DetailedSleepAnalysis(
    val bedTime: String? = null,
    val wakeUpTime: String? = null,
    val sleepMinutes: Int? = null,
    
    // Durations
    val awakeMinutes: Int? = null,
    val remSleepMinutes: Int? = null,
    val lightSleepMinutes: Int? = null,
    val deepSleepMinutes: Int? = null,
    
    // Percentages
    val awakePercent: Double? = null,
    val remSleepPercent: Double? = null,
    val lightSleepPercent: Double? = null,
    val deepSleepPercent: Double? = null
)

// 5. The Root Object
data class HealthData(
    // High level totals for the day
    val totalSteps: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalActiveMinutes: Int = 0,
    
    val biometrics: UserBiometrics = UserBiometrics(),
    val vitals: Vitals = Vitals(),
    val sleepAnalysis: DetailedSleepAnalysis = DetailedSleepAnalysis(),
    
    // The list of individual sessions
    val exerciseSessionsList: List<ExerciseSessionDetails> = emptyList()
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}
