package com.tps.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tps.data.remote.dto.ApiResponse
import com.tps.data.remote.dto.LoginResponse
import com.tps.data.remote.dto.RefreshTokenRequest
import com.tps.util.TokenManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class AuthRefreshAuthenticator(
    private val tokenManager: TokenManager,
    private val gson: Gson
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder().build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith("/api/auth/refresh")) return null
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenManager.getRefreshToken() ?: return null
        synchronized(this) {
            val currentToken = tokenManager.getToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshed = refreshToken(response.request, refreshToken) ?: return null
            tokenManager.saveToken(refreshed.token)
            tokenManager.saveRefreshToken(refreshed.refreshToken)
            tokenManager.saveUserId(refreshed.userId)
            tokenManager.saveRole(refreshed.role)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.token}")
                .build()
        }
    }

    private fun refreshToken(originalRequest: Request, refreshToken: String): LoginResponse? {
        val refreshUrl = NetworkEndpointConfig.primaryApiBaseUrl
            .newBuilder()
            .addPathSegments("api/auth/refresh")
            .build()
        val body = gson.toJson(RefreshTokenRequest(refreshToken))
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(refreshUrl)
            .post(body)
            .build()

        return try {
            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string() ?: return null
                val type = object : TypeToken<ApiResponse<LoginResponse>>() {}.type
                val parsed: ApiResponse<LoginResponse> = gson.fromJson(responseBody, type)
                parsed.data
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
