package com.healthpipeline.config

/**
 * API Configuration
 * 
 * IMPORTANT: Update these URLs based on your environment:
 * 
 * - For Android Emulator: use "10.0.2.2" (maps to host machine's localhost)
 * - For Physical Device: use your computer's local IP (e.g., "192.168.31.175")
 * - For Production: use your actual domain
 * 
 * To find your local IP:
 * - Windows: ipconfig
 * - Mac/Linux: ifconfig or hostname -I
 */
object ApiConfig {
    // 🔧 DEVELOPMENT: Use localhost with ADB reverse
    // Run: adb reverse tcp:5000 tcp:5000 && adb reverse tcp:8000 tcp:8000
    private const val DEV_HOST = "127.0.0.1"
    
    // 🚀 PRODUCTION: Separate IAM and FHIR Servers
    const val IAM_SERVER = "https://iam.drgodly.com"
    const val FHIR_SERVER = "https://fhir.drgodly.com"
    
    // Toggle between dev and production
    private const val USE_PRODUCTION = true // Set to false for local development
    
    // Backend API (FastAPI - Health Data / FHIR)
    val HEALTH_API_BASE_URL = if (USE_PRODUCTION) "$FHIR_SERVER/" else "http://$DEV_HOST:8000/"
    
    // IAM Server (Authentication)
    val AUTH_API_BASE_URL = if (USE_PRODUCTION) "$IAM_SERVER/api/auth/" else "http://$DEV_HOST:5000/api/auth/"
    
    // FHIR API Endpoints
    val FHIR_VITALS_URL = "${HEALTH_API_BASE_URL}api/v1/vitals/"
    val FHIR_PATIENTS_URL = "${HEALTH_API_BASE_URL}api/fhir/v1/patients/"
    val FHIR_MY_VITALS_URL = "${HEALTH_API_BASE_URL}api/v1/vitals/me"
    val FHIR_MY_PATIENT_URL = "${HEALTH_API_BASE_URL}api/fhir/v1/patients/me"
    
    // IAM Authentication Endpoints
    val IAM_SIGN_IN_URL = "${AUTH_API_BASE_URL}sign-in/email"
    val IAM_SIGN_UP_URL = "${AUTH_API_BASE_URL}sign-up/email"
    val IAM_GET_SESSION_URL = "${AUTH_API_BASE_URL}get-session"
    val IAM_SIGN_OUT_URL = "${AUTH_API_BASE_URL}sign-out"
    
    // OAuth Configuration (Fallback - not used with direct auth)
    val OAUTH_AUTHORIZE_URL = "${AUTH_API_BASE_URL}oauth2/authorize"
    val OAUTH_TOKEN_URL = "${AUTH_API_BASE_URL}oauth2/token"
    const val OAUTH_CLIENT_ID = "phia-mobile-app"
    const val OAUTH_REDIRECT_URI = "phia://auth/callback"
    
    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 60L
}
