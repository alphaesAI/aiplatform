package com.healthpipeline.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages user sessions, tokens, and identity (UUID).
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "phia_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"  // Short token from sign-in
        private const val KEY_JWT_TOKEN = "jwt_token"        // JWT for FHIR access
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CODE_VERIFIER = "code_verifier"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }

    /**
     * Saves the PKCE code verifier temporarily.
     */
    fun saveCodeVerifier(verifier: String) {
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()
    }

    /**
     * Retrieves and CLEARS the PKCE code verifier.
     */
    fun getAndClearCodeVerifier(): String? {
        val verifier = prefs.getString(KEY_CODE_VERIFIER, null)
        prefs.edit().remove(KEY_CODE_VERIFIER).apply()
        return verifier
    }

    /**
     * Saves the user session data.
     */
    fun saveSession(userId: String, accessToken: String?) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * Saves user email.
     */
    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }
    
    /**
     * Saves user name.
     */
    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }
    
    /**
     * Gets user email.
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Gets user name.
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Gets the unique User ID (UUID).
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Gets the short session token (from sign-in).
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Saves the JWT token for FHIR access.
     */
    fun saveJWTToken(jwtToken: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, jwtToken).apply()
    }
    
    /**
     * Gets the JWT token for FHIR access.
     */
    fun getJWTToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    /**
     * Checks if the user is authenticated.
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Clears the session (Logout).
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Saves all user data at once (Step 1: after sign-in).
     */
    fun saveUserData(userId: String, accessToken: String, email: String, name: String?) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * Saves all user data with JWT (Step 2: after token exchange).
     */
    fun saveUserDataWithJWT(userId: String, shortToken: String, jwtToken: String, email: String, name: String?) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, shortToken)
            putString(KEY_JWT_TOKEN, jwtToken)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
}
