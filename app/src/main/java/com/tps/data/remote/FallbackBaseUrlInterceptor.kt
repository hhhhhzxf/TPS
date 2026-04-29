package com.tps.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class FallbackBaseUrlInterceptor(
    private val baseUrls: List<HttpUrl>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastException: IOException? = null

        baseUrls.forEachIndexed { index, baseUrl ->
            val request = if (index == 0) {
                originalRequest
            } else {
                originalRequest.newBuilder()
                    .url(originalRequest.url.rewriteBaseUrl(baseUrl))
                    .build()
            }

            try {
                return chain.proceed(request)
            } catch (exception: IOException) {
                lastException = exception
            }
        }

        throw lastException ?: IOException("No API base URL configured")
    }

    private fun HttpUrl.rewriteBaseUrl(baseUrl: HttpUrl): HttpUrl =
        newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()
}
