package com.healthpipeline.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds the appropriate Bearer token to outgoing requests.
 * - Uses JWT token for FHIR server (fhir.drgodly.com)
 * - Uses short session token for IAM server (iam.drgodly.com)
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Determine which token to use based on the URL
        val token = when {
            // FHIR server uses JWT token
            url.contains("fhir.drgodly.com") -> {
                val jwt = sessionManager.getJWTToken()
                Log.d("AuthInterceptor", "🔑 FHIR request - JWT: ${jwt?.take(20)}... (${if(jwt!=null)"PRESENT"else"NULL"})")
                jwt
            }
            // IAM server uses short session token
            url.contains("iam.drgodly.com") -> {
                val access = sessionManager.getAccessToken()
                Log.d("AuthInterceptor", "🔑 IAM request - Access token: ${access?.take(20)}... (${if(access!=null)"PRESENT"else"NULL"})")
                access
            }
            // Default: use short token
            else -> sessionManager.getAccessToken()
        }

        // If we have a token, add it to the header
        return if (token != null) {
            Log.d("AuthInterceptor", "✅ Adding Authorization header to request")
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            Log.e("AuthInterceptor", "❌ NO TOKEN AVAILABLE - Request will fail!")
            chain.proceed(originalRequest)
        }
    }
}
