package com.healthpipeline.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthpipeline.config.ApiConfig
import com.healthpipeline.data.HealthApiService
import com.healthpipeline.data.SessionCookieJar
import com.healthpipeline.data.SignInRequest
import com.healthpipeline.data.SignUpRequest
import com.healthpipeline.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val userId: String? = null,
    val accessToken: String? = null,
    val userEmail: String? = null,
    val userName: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sessionManager = SessionManager(application)
    private val _uiState = MutableStateFlow(AuthUiState(
        isSuccess = sessionManager.isLoggedIn(),
        userId = sessionManager.getUserId(),
        accessToken = sessionManager.getAccessToken()
    ))
    val uiState: StateFlow<AuthUiState> = _uiState

    // 🍪 Shared cookie jar for auth requests (must persist cookies between sign-in and JWT exchange)
    private val cookieJar = SessionCookieJar()
    
    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()
    
    private val authApi = Retrofit.Builder()
        .baseUrl(ApiConfig.AUTH_API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HealthApiService::class.java)

    // ==================== IAM EMAIL/PASSWORD AUTH ====================
    
    /**
     * 🔐 Sign In with Email and Password
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val request = SignInRequest(
                    email = email.trim(),
                    password = password,
                    rememberMe = true
                )
                
                Log.d("AuthViewModel", "🔐 Signing in: $email")
                val response = authApi.signInWithEmail(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val shortToken = body.token
                        val user = body.user
                        
                        Log.d("AuthViewModel", "✅ Sign In Success. User: ${user.id}")
                        
                        // 🔐 STEP 2: Exchange short token for JWT
                        Log.d("AuthViewModel", "🔐 Exchanging short token for JWT...")
                        val jwtResponse = authApi.exchangeForJWT("Bearer $shortToken")
                        
                        val jwtToken = if (jwtResponse.isSuccessful) {
                            val token = jwtResponse.body()?.jwtToken
                            Log.d("AuthViewModel", "✅ JWT exchange SUCCESS - Token: ${token?.take(30)}...")
                            token
                        } else {
                            val error = jwtResponse.errorBody()?.string()
                            Log.e("AuthViewModel", "❌ JWT exchange FAILED: ${jwtResponse.code()} - $error")
                            null
                        }
                        
                        // 🔐 PERSIST SESSION with both tokens
                        if (jwtToken != null) {
                            sessionManager.saveUserDataWithJWT(user.id, shortToken, jwtToken, user.email, user.name)
                            Log.d("AuthViewModel", "✅ JWT obtained and saved")
                        } else {
                            // Fallback: save without JWT
                            sessionManager.saveSession(user.id, shortToken)
                            sessionManager.saveUserEmail(user.email)
                            sessionManager.saveUserName(user.name ?: "")
                        }

                        _uiState.value = AuthUiState(
                            isSuccess = true, 
                            userId = user.id,
                            accessToken = shortToken,
                            userEmail = user.email,
                            userName = user.name
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(error = "Invalid response from server")
                    }
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("AuthViewModel", "❌ Sign In Failed: $err")
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid email or password"
                        404 -> "User not found"
                        429 -> "Too many attempts. Please try again later"
                        else -> "Sign in failed. Please try again"
                    }
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Sign In Error", e)
                _uiState.value = _uiState.value.copy(error = "Connection error. Check your internet.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 🔐 Sign Up with Email, Password, and Name
     */
    fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all fields")
            return
        }
        
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 8 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val request = SignUpRequest(
                    email = email.trim(),
                    password = password,
                    name = name.trim(),
                    rememberMe = true
                )
                
                Log.d("AuthViewModel", "🔐 Signing up: $email")
                val response = authApi.signUpWithEmail(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val shortToken = body.token
                        val user = body.user
                        
                        Log.d("AuthViewModel", "✅ Sign Up Success. User: ${user.id}")
                        
                        // 🔐 PERSIST SESSION (token might be null for email verification)
                        if (shortToken != null) {
                            // 🔐 STEP 2: Exchange short token for JWT
                            Log.d("AuthViewModel", "🔐 Exchanging short token for JWT...")
                            val jwtResponse = authApi.exchangeForJWT("Bearer $shortToken")
                            
                            val jwtToken = if (jwtResponse.isSuccessful) {
                                jwtResponse.body()?.jwtToken
                            } else {
                                Log.e("AuthViewModel", "❌ JWT exchange failed: ${jwtResponse.errorBody()?.string()}")
                                null
                            }
                            
                            // 🔐 PERSIST SESSION with both tokens
                            if (jwtToken != null) {
                                sessionManager.saveUserDataWithJWT(user.id, shortToken, jwtToken, user.email, user.name)
                                Log.d("AuthViewModel", "✅ JWT obtained and saved")
                            } else {
                                // Fallback: save without JWT
                                sessionManager.saveSession(user.id, shortToken)
                                sessionManager.saveUserEmail(user.email)
                                sessionManager.saveUserName(user.name ?: "")
                            }
                            
                            _uiState.value = AuthUiState(
                                isSuccess = true, 
                                userId = user.id,
                                accessToken = shortToken,
                                userEmail = user.email,
                                userName = user.name
                            )
                        } else {
                            // Email verification required
                            _uiState.value = _uiState.value.copy(
                                error = "Please check your email to verify your account"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(error = "Invalid response from server")
                    }
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("AuthViewModel", "❌ Sign Up Failed: $err")
                    val errorMsg = when (response.code()) {
                        422 -> "Email already registered"
                        429 -> "Too many attempts. Please try again later"
                        else -> "Sign up failed. Please try again"
                    }
                    _uiState.value = _uiState.value.copy(error = errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Sign Up Error", e)
                _uiState.value = _uiState.value.copy(error = "Connection error. Check your internet.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 🔐 Validate current session/token
     */
    fun validateSession() {
        val token = sessionManager.getAccessToken() ?: return
        
        viewModelScope.launch {
            try {
                val response = authApi.getSession("Bearer $token")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.session != null && body.user != null) {
                        // Session is valid
                        Log.d("AuthViewModel", "✅ Session valid for user: ${body.user.id}")
                        _uiState.value = _uiState.value.copy(
                            isSuccess = true,
                            userId = body.user.id,
                            accessToken = token,
                            userEmail = body.user.email,
                            userName = body.user.name
                        )
                    } else {
                        // Session expired
                        Log.d("AuthViewModel", "⚠️ Session expired")
                        logout()
                    }
                } else {
                    Log.d("AuthViewModel", "⚠️ Session invalid: ${response.code()}")
                    logout()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Session validation error", e)
                // Don't logout on network error, keep cached session
            }
        }
    }
    
    /**
     * 🔐 Logout
     */
    fun logout() {
        val token = sessionManager.getAccessToken()
        
        viewModelScope.launch {
            try {
                // Call sign-out endpoint
                if (token != null) {
                    authApi.signOut("Bearer $token")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign out error", e)
            } finally {
                // Clear local session
                sessionManager.logout()
                _uiState.value = AuthUiState(isSuccess = false)
            }
        }
    }

    fun setAuthenticated(success: Boolean) {
        if (!success) {
            sessionManager.logout()
        }
        _uiState.value = AuthUiState(isSuccess = success)
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ==================== LEGACY OAUTH2 (Not Used) ====================
    
    @Deprecated("Use signIn() instead")
    fun generatePKCE(): Pair<String, String> = "" to ""

    @Deprecated("Use signIn() instead")
    fun onAuthCodeReceived(code: String?) {
        _uiState.value = _uiState.value.copy(error = "OAuth2 not supported. Use email/password.")
    }

    @Deprecated("Use logout() instead")
    fun getLogoutUrl(): String = ""
}
