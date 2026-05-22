package com.healthpipeline.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthpipeline.data.models.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    )

    suspend fun checkPermissions(): Pair<String, Boolean> {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            val hasAllPermissions = permissions.all { it in grantedPermissions }
            if (hasAllPermissions) "All permissions granted" to true else "Permissions needed" to false
        } catch (e: Exception) {
            "Error: ${e.message}" to false
        }
    }
    
    suspend fun readHealthData(): HealthData {
        return try {
            val steps = readTodaySteps()
            var calories = readTodayCalories()
            val distance = readTodayDistance()
            val exerciseList = readTodayExercise() 
        
            var totalActiveMinutes = exerciseList.sumOf { it.activeZoneMinutes }
        
            val vitals = readVitals()
            val sleepAnalysis = readDetailedSleepAnalysis()
            val biometrics = readBiometrics()
            
            // Calculate estimated calories if no data from Health Connect
            if (calories == 0 && steps > 0) {
                val weight = biometrics.weightKg ?: 70.0 // Default 70kg if no weight data
                // Formula: steps * weight * 0.0005 (approx 0.04 kcal per step for average person)
                val estimatedCalories = (steps * weight * 0.0005).toInt()
                calories = estimatedCalories
                Log.d("HealthConnect", "📊 Estimated calories: $calories kcal (from $steps steps, ${weight}kg)")
            }
            
            // Estimate active minutes if no exercise data (approx 100 steps = 1 active minute)
            if (totalActiveMinutes == 0 && steps > 0) {
                totalActiveMinutes = steps / 100
                Log.d("HealthConnect", "📊 Estimated active minutes: $totalActiveMinutes min (from $steps steps)")
            }
        
            HealthData(
                totalSteps = steps,
                totalCaloriesBurned = calories,
                totalDistanceKm = distance,
                totalActiveMinutes = totalActiveMinutes,
                biometrics = biometrics,
                vitals = vitals,
                sleepAnalysis = sleepAnalysis,
                exerciseSessionsList = exerciseList
            )
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading data", e)
            HealthData()
        }
    }
    
    private suspend fun readTodaySteps(): Int {
        return try {
            val startOfDay = LocalDateTime.now().withHour(0).withMinute(0).atZone(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            Log.d("HealthConnect", "📊 Reading steps from ${startOfDay} to ${now}")
            
            val response = healthConnectClient.readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startOfDay, now)))
            
            Log.d("HealthConnect", "📊 Found ${response.records.size} step records:")
            response.records.forEachIndexed { index, record ->
                val source = record.metadata.dataOrigin.packageName
                Log.d("HealthConnect", "   Record $index: ${record.count} steps from $source (${record.startTime} to ${record.endTime})")
            }
            
            // Filter to only Google Fit data
            val googleFitRecords = response.records.filter { 
                it.metadata.dataOrigin.packageName.contains("google.android.apps.fitness", ignoreCase = true) 
            }
            
            val totalSteps = googleFitRecords.sumOf { it.count.toInt() }
            Log.d("HealthConnect", "📊 Google Fit steps only: $totalSteps (filtered from ${response.records.size} total records)")
            totalSteps
        } catch (e: Exception) { 
            Log.e("HealthConnect", "❌ Error reading steps", e)
            0 
        }
    }

    private suspend fun readTodayDistance(): Double {
        return try {
            val startOfDay = LocalDateTime.now().withHour(0).withMinute(0).atZone(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            
            val response = healthConnectClient.readRecords(ReadRecordsRequest(DistanceRecord::class, TimeRangeFilter.between(startOfDay, now)))
            
            Log.d("HealthConnect", "📊 Found ${response.records.size} distance records:")
            response.records.forEachIndexed { index, record ->
                val source = record.metadata.dataOrigin.packageName
                Log.d("HealthConnect", "   Record $index: ${record.distance.inMeters}m from $source (${record.startTime} to ${record.endTime})")
            }
            
            // Filter to only Google Fit data
            val googleFitRecords = response.records.filter { 
                it.metadata.dataOrigin.packageName.contains("google.android.apps.fitness", ignoreCase = true) 
            }
            
            val totalKm = googleFitRecords.sumOf { it.distance.inMeters } / 1000.0
            Log.d("HealthConnect", "📊 Google Fit distance only: ${totalKm}km (filtered from ${response.records.size} total records)")
            totalKm
        } catch (e: Exception) { 
            Log.e("HealthConnect", "❌ Error reading distance", e)
            0.0 
        }
    }

    private suspend fun readTodayCalories(): Int {
        return try {
            val startOfDay = LocalDateTime.now().withHour(0).withMinute(0).atZone(ZoneId.systemDefault()).toInstant()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(startOfDay, Instant.now())))
            
            Log.d("HealthConnect", "📊 Found ${response.records.size} calorie records")
            response.records.forEachIndexed { index, record ->
                val source = record.metadata.dataOrigin.packageName
                Log.d("HealthConnect", "   Record $index: ${record.energy.inCalories} kcal from $source")
            }
            
            val totalCalories = response.records.sumOf { it.energy.inCalories }.toInt()
            Log.d("HealthConnect", "📊 Total calories: $totalCalories kcal")
            totalCalories
        } catch (e: Exception) { 
            Log.e("HealthConnect", "❌ Error reading calories", e)
            0 
        }
    }

    private suspend fun readTodayExercise(): List<ExerciseSessionDetails> {
        return try {
            val startOfDay = LocalDateTime.now().withHour(0).withMinute(0).atZone(ZoneId.systemDefault()).toInstant()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(startOfDay, Instant.now())))
            
            Log.d("HealthConnect", "📊 Found ${response.records.size} exercise sessions")
            response.records.forEachIndexed { index, record ->
                val durationMins = ((record.endTime.toEpochMilli() - record.startTime.toEpochMilli()) / 60000).toLong()
                Log.d("HealthConnect", "   Exercise $index: ${record.exerciseType}, ${durationMins}min from ${record.metadata.dataOrigin.packageName}")
            }
            
            response.records.map { record ->
                val durationMins = ((record.endTime.toEpochMilli() - record.startTime.toEpochMilli()) / 60000).toLong()
                ExerciseSessionDetails(
                    activityName = record.exerciseType.toString(),
                    startTime = record.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                    endTime = record.endTime.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                    duration = durationMins,
                    activeZoneMinutes = durationMins.toInt(), // Fallback estimation
                    fatburnActiveZoneMinutes = (durationMins * 0.5).toInt(),
                    cardioActiveZoneMinutes = (durationMins * 0.3).toInt(),
                    peakActiveZoneMinutes = (durationMins * 0.2).toInt()
                )
            }
        } catch (e: Exception) { 
            Log.e("HealthConnect", "❌ Error reading exercise sessions", e)
            emptyList() 
        }
    }

    private suspend fun readVitals(): Vitals {
        return try {
            val startOfDay = LocalDateTime.now().withHour(0).withMinute(0).atZone(ZoneId.systemDefault()).toInstant()
            
            // RHR
            var rhr: Int? = null
            var rhrSource = "none"
            try {
                val rhrResponse = healthConnectClient.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, TimeRangeFilter.between(startOfDay.minus(7, ChronoUnit.DAYS), Instant.now())))
                Log.d("HealthConnect", "📊 Found ${rhrResponse.records.size} RHR records")
                rhrResponse.records.lastOrNull()?.let {
                    rhr = it.beatsPerMinute.toInt()
                    rhrSource = it.metadata.dataOrigin.packageName
                    Log.d("HealthConnect", "   RHR: ${rhr} bpm from ${rhrSource}")
                }
            } catch(e: Exception){
                Log.e("HealthConnect", "❌ Error reading RHR", e)
            }
            
            // HRV
            var hrv: Double? = null
            var hrvSource = "none"
            try {
                val hrvResponse = healthConnectClient.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, TimeRangeFilter.between(startOfDay.minus(7, ChronoUnit.DAYS), Instant.now())))
                Log.d("HealthConnect", "📊 Found ${hrvResponse.records.size} HRV records")
                hrvResponse.records.lastOrNull()?.let {
                    hrv = it.heartRateVariabilityMillis
                    hrvSource = it.metadata.dataOrigin.packageName
                    Log.d("HealthConnect", "   HRV: ${hrv} ms from ${hrvSource}")
                }
            } catch(e: Exception){
                Log.e("HealthConnect", "❌ Error reading HRV", e)
            }

            Log.d("HealthConnect", "📊 Vitals - RHR: ${rhr} bpm (${rhrSource}), HRV: ${hrv} ms (${hrvSource})")
            Vitals(
                restingHeartRate = rhr,
                heartRateVariability = hrv,
                stressManagementScore = null
            )
        } catch (e: Exception) { 
            Log.e("HealthConnect", "❌ Error reading vitals", e)
            Vitals(
                restingHeartRate = null,
                heartRateVariability = null,
                stressManagementScore = null
            ) 
        }
    }

    private suspend fun readDetailedSleepAnalysis(): DetailedSleepAnalysis {
        try {
            val startTime = Instant.now().minus(24, ChronoUnit.HOURS)
            val response = healthConnectClient.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(startTime, Instant.now())))
            
            Log.d("HealthConnect", "📊 Found ${response.records.size} sleep sessions")
            
            val latestSession = response.records.lastOrNull() 
            if (latestSession == null) {
                Log.d("HealthConnect", "📊 No sleep sessions found")
                return DetailedSleepAnalysis()
            }
            
            Log.d("HealthConnect", "📊 Latest sleep session: ${latestSession.startTime} to ${latestSession.endTime}, stages: ${latestSession.stages.size}")
            
            var awake = 0
            var rem = 0
            var light = 0
            var deep = 0
            
            latestSession.stages.forEach { stage ->
                val duration = ((stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()) / 60000).toInt()
                when(stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += duration
                    SleepSessionRecord.STAGE_TYPE_REM -> rem += duration
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> light += duration
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deep += duration
                    else -> light += duration
                }
            }
            
            val totalMins = awake + rem + light + deep
            Log.d("HealthConnect", "📊 Sleep breakdown - Total: ${totalMins}min, Awake: ${awake}min, REM: ${rem}min, Light: ${light}min, Deep: ${deep}min")
            
            if (totalMins == 0) {
                Log.d("HealthConnect", "📊 No sleep stages data")
                return DetailedSleepAnalysis()
            }
            
            return DetailedSleepAnalysis(
                bedTime = latestSession.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                wakeUpTime = latestSession.endTime.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                sleepMinutes = totalMins - awake,
                awakeMinutes = awake,
                remSleepMinutes = rem,
                lightSleepMinutes = light,
                deepSleepMinutes = deep,
                awakePercent = (awake.toDouble() / totalMins) * 100,
                remSleepPercent = (rem.toDouble() / totalMins) * 100,
                lightSleepPercent = (light.toDouble() / totalMins) * 100,
                deepSleepPercent = (deep.toDouble() / totalMins) * 100
            )
        } catch (e: Exception) { return DetailedSleepAnalysis() }
    }
    
    private suspend fun readBiometrics(): UserBiometrics {
        return try {
            val startTime = Instant.now().minus(365, ChronoUnit.DAYS)
            
            var weight: Double? = null
            var weightSource: String = "none"
            try {
                val weightResponse = healthConnectClient.readRecords(ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(startTime, Instant.now())))
                Log.d("HealthConnect", "📊 Found ${weightResponse.records.size} weight records")
                weightResponse.records.lastOrNull()?.let { 
                    weight = it.weight.inKilograms
                    weightSource = it.metadata.dataOrigin.packageName
                    Log.d("HealthConnect", "   Weight: ${weight}kg from $weightSource")
                }
            } catch(e: Exception){
                Log.e("HealthConnect", "❌ Error reading weight", e)
            }
            
            var height: Double? = null
            var heightSource: String = "none"
            try {
                val heightResponse = healthConnectClient.readRecords(ReadRecordsRequest(HeightRecord::class, TimeRangeFilter.between(startTime, Instant.now())))
                Log.d("HealthConnect", "📊 Found ${heightResponse.records.size} height records")
                heightResponse.records.lastOrNull()?.let { 
                    height = it.height.inMeters?.times(100)
                    heightSource = it.metadata.dataOrigin.packageName
                    Log.d("HealthConnect", "   Height: ${height}cm from $heightSource")
                }
            } catch(e: Exception){
                Log.e("HealthConnect", "❌ Error reading height", e)
            }
            
            Log.d("HealthConnect", "📊 Biometrics - Weight: ${weight}kg (${weightSource}), Height: ${height}cm (${heightSource})")
            
            UserBiometrics(
                age = null,
                gender = null,
                weightKg = weight,
                heightCm = height
            )
        } catch (e: Exception) { 
            Log.e("HealthConnect", "❌ Error reading biometrics", e)
            UserBiometrics() 
        }
    }
}