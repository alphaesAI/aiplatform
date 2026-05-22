package com.healthpipeline.data

import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import com.healthpipeline.data.models.HealthQueueRequest
import com.healthpipeline.data.models.HealthQueueResponse
import com.healthpipeline.data.models.QueueStatusResponse

// --- OAuth2 Models ---
data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("id_token") val idToken: String?,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long
)

// --- IAM Authentication Models ---
// Sign In Response
data class SignInResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserData
)

// Sign Up Response
data class SignUpResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("user") val user: UserData
)

// User Data
data class UserData(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("emailVerified") val emailVerified: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

// Session Response
data class SessionResponse(
    @SerializedName("session") val session: SessionData?,
    @SerializedName("user") val user: UserData?
)

data class SessionData(
    @SerializedName("id") val id: String,
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("expiresAt") val expiresAt: String
)

// Sign In/Up Request
data class SignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("rememberMe") val rememberMe: Boolean = true
)

data class SignUpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("name") val name: String,
    @SerializedName("rememberMe") val rememberMe: Boolean = true
)

// JWT Token Response (Step 2 of 2-step auth)
data class JWTTokenResponse(
    @SerializedName("token") val jwtToken: String
)

// ==================== FHIR DATA MODELS ====================

// Vitals Request (POST /api/v1/vitals/)
// Includes ALL fields collected from Health Connect
data class VitalsRequest(
    // Core Activity Metrics
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("calories_kcal") val caloriesKcal: Double? = null,
    @SerializedName("distance_meters") val distanceMeters: Double? = null,
    @SerializedName("total_active_minutes") val totalActiveMinutes: Int? = null,
    
    // Exercise Data
    @SerializedName("activity_name") val activityName: String? = null,
    @SerializedName("exercise_duration_minutes") val exerciseDurationMinutes: Double? = null,
    @SerializedName("active_zone_minutes") val activeZoneMinutes: Int? = null,
    @SerializedName("fatburn_active_zone_minutes") val fatburnActiveZoneMinutes: Int? = null,
    @SerializedName("cardio_active_zone_minutes") val cardioActiveZoneMinutes: Int? = null,
    @SerializedName("peak_active_zone_minutes") val peakActiveZoneMinutes: Int? = null,
    
    // Vitals
    @SerializedName("resting_heart_rate") val restingHeartRate: Int? = null,
    @SerializedName("heart_rate") val heartRate: Int? = null,
    @SerializedName("heart_rate_variability") val heartRateVariability: Double? = null,
    @SerializedName("stress_management_score") val stressManagementScore: Int? = null,
    @SerializedName("blood_pressure_systolic") val bloodPressureSystolic: Int? = null,
    @SerializedName("blood_pressure_diastolic") val bloodPressureDiastolic: Int? = null,
    
    // Sleep Data
    @SerializedName("sleep_minutes") val sleepMinutes: Int? = null,
    @SerializedName("rem_sleep_minutes") val remSleepMinutes: Int? = null,
    @SerializedName("deep_sleep_minutes") val deepSleepMinutes: Int? = null,
    @SerializedName("light_sleep_minutes") val lightSleepMinutes: Int? = null,
    @SerializedName("awake_minutes") val awakeMinutes: Int? = null,
    @SerializedName("bed_time") val bedTime: String? = null,
    @SerializedName("wake_up_time") val wakeUpTime: String? = null,
    @SerializedName("deep_sleep_percent") val deepSleepPercent: Double? = null,
    @SerializedName("rem_sleep_percent") val remSleepPercent: Double? = null,
    @SerializedName("light_sleep_percent") val lightSleepPercent: Double? = null,
    @SerializedName("awake_percent") val awakePercent: Double? = null,
    
    // Biometrics
    @SerializedName("weight_kg") val weightKg: Double? = null,
    @SerializedName("height_cm") val heightCm: Double? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("gender") val gender: String? = null,
    
    // Metadata (server auto-sets date, don't send it)
    @SerializedName("recorded_at") val recordedAt: String? = null
)

// Vitals Response (from GET/POST /api/v1/vitals/)
data class VitalRecord(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("heart_rate") val heartRate: Int? = null,
    @SerializedName("blood_pressure_systolic") val bloodPressureSystolic: Int? = null,
    @SerializedName("blood_pressure_diastolic") val bloodPressureDiastolic: Int? = null,
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("calories") val calories: Double? = null,
    @SerializedName("sleep_minutes") val sleepMinutes: Int? = null,
    @SerializedName("recorded_at") val recordedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// Patient Profile (GET /api/fhir/v1/patients/me)
data class PatientRecord(
    @SerializedName("id") val id: String? = null,
    @SerializedName("resourceType") val resourceType: String? = null,
    @SerializedName("name") val name: List<PatientName>? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("birthDate") val birthDate: String? = null
)

data class PatientName(
    @SerializedName("use") val use: String? = null,
    @SerializedName("family") val family: String? = null,
    @SerializedName("given") val given: List<String>? = null
)

// API Service Interface
interface HealthApiService {
    
    // ==================== IAM AUTHENTICATION ====================
    
    // 🔐 Sign In with Email/Password
    @Headers("Content-Type: application/json")
    @POST("https://iam.drgodly.com/api/auth/sign-in/email")
    suspend fun signInWithEmail(
        @Body request: SignInRequest
    ): Response<SignInResponse>
    
    // 🔐 Sign Up with Email/Password
    @Headers("Content-Type: application/json")
    @POST("https://iam.drgodly.com/api/auth/sign-up/email")
    suspend fun signUpWithEmail(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>
    
    // 🔐 Get Current Session (validate token)
    @GET("https://iam.drgodly.com/api/auth/get-session")
    suspend fun getSession(
        @Header("Authorization") authorization: String
    ): Response<SessionResponse>
    
    // 🔐 Sign Out
    @Headers("Content-Type: application/json")
    @POST("https://iam.drgodly.com/api/auth/sign-out")
    suspend fun signOut(
        @Header("Authorization") authorization: String? = null
    ): Response<Map<String, Boolean>>
    
    // 🔐 Exchange Short Token for JWT Token (for FHIR access)
    // MUST use full URL to call IAM server, not FHIR server
    @GET("https://iam.drgodly.com/api/auth/token")
    suspend fun exchangeForJWT(
        @Header("Authorization") authorization: String
    ): Response<JWTTokenResponse>
    
    // ==================== FHIR HEALTH DATA API ====================
    
    // � GET my vitals (current user)
    @GET("api/v1/vitals/me")
    suspend fun getMyVitals(): Response<List<VitalRecord>>
    
    // 🔥 POST new vitals
    @Headers("Content-Type: application/json")
    @POST("api/v1/vitals/")
    suspend fun createVitals(
        @Body request: VitalsRequest
    ): Response<VitalRecord>
    
    // 🔥 GET my patient profile
    @GET("api/fhir/v1/patients/me")
    suspend fun getMyPatientProfile(): Response<PatientRecord>
    
    // ==================== LEGACY (Deprecated) ====================
    
    // 🚀 DEPRECATED: Use FHIR endpoints above instead
    @Deprecated("Use createVitals() instead")
    @POST("api/health/queue")
    suspend fun queueHealthData(
        @Body request: HealthQueueRequest
    ): Response<HealthQueueResponse>
    
    @Deprecated("Not needed with FHIR")
    @GET("api/health/status/{queueId}")
    suspend fun getQueueStatus(
        @Path("queueId") queueId: String
    ): Response<QueueStatusResponse>
    
    // Health check endpoint
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
    
    // ==================== OAUTH2 (Legacy/Fallback) ====================
    
    @Deprecated("Use sign-in/email instead")
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun exchangeCodeForToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String = "phia://auth/callback",
        @Field("client_id") clientId: String = "phia-mobile-app",
        @Field("code_verifier") codeVerifier: String?
    ): Response<TokenResponse>
}
