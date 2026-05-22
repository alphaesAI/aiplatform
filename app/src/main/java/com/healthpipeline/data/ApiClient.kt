package com.healthpipeline.data

import android.content.Context
import android.util.Log
import com.healthpipeline.config.ApiConfig
import com.healthpipeline.utils.AuthInterceptor
import com.healthpipeline.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Simple in-memory cookie jar to persist session cookies
 * Essential for IAM server authentication flow
 */
class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.removeAll { existing ->
            cookies.any { new -> new.name == existing.name && new.domain == existing.domain }
        }
        this.cookies.addAll(cookies)
        Log.d("SessionCookieJar", "🍪 Saved ${cookies.size} cookies for ${url.host}")
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val matchingCookies = cookies.filter { it.matches(url) }
        Log.d("SessionCookieJar", "🍪 Loaded ${matchingCookies.size} cookies for ${url.host}")
        return matchingCookies
    }
}

class ApiClient(private val context: Context) {
    
    companion object {
        private const val TAG = "ApiClient"
    }

    private val sessionManager = SessionManager(context)
    
    // Shared cookie jar for all requests (maintains session across IAM and FHIR)
    private val cookieJar = SessionCookieJar()
    
    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.HEALTH_API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val healthApiService: HealthApiService = retrofit.create(HealthApiService::class.java)
    
    // Helper method to handle API calls with error handling
    suspend fun <T> safeApiCall(
        apiCall: suspend () -> retrofit2.Response<T>
    ): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        ApiResult.Success(body)
                    } ?: ApiResult.Error("Empty response body")
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: ${response.code()} - $errorMessage")
                    ApiResult.Error("API Error: ${response.code()} - $errorMessage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Error: ${e.message}", e)
                ApiResult.Error("Network Error: ${e.message}")
            }
        }
    }
    
    // Test API connectivity
    suspend fun testConnection(): ApiResult<Boolean> {
        return try {
            val result = safeApiCall { healthApiService.healthCheck() }
            when (result) {
                is ApiResult.Success -> {
                    Log.i(TAG, "API connection successful")
                    ApiResult.Success(true)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "API connection failed: ${result.message}")
                    ApiResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            ApiResult.Error("Connection test failed: ${e.message}")
        }
    }
}

// Result wrapper for API calls
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

// Extension function to check if result is successful
fun <T> ApiResult<T>.isSuccess(): Boolean = this is ApiResult.Success

// Extension function to get data or null
fun <T> ApiResult<T>.getDataOrNull(): T? = when (this) {
    is ApiResult.Success -> data
    is ApiResult.Error -> null
}
