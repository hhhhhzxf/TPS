package com.tps.data.remote

import com.tps.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object NetworkEndpointConfig {
    val apiBaseUrls: List<HttpUrl> = buildList {
        addNormalized(BuildConfig.BASE_URL)
        BuildConfig.FALLBACK_BASE_URLS
            .split(",")
            .forEach { addNormalized(it) }
    }.distinct()

    val websocketUrls: List<String> = buildList {
        addNormalizedWebSocket(BuildConfig.WS_URL)
        BuildConfig.FALLBACK_WS_URLS
            .split(",")
            .forEach { addNormalizedWebSocket(it) }
    }.distinct()

    val primaryApiBaseUrl: HttpUrl = apiBaseUrls.first()

    private fun MutableList<HttpUrl>.addNormalized(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) return
        add(normalized.trimEnd('/').plus("/").toHttpUrl())
    }

    private fun MutableList<String>.addNormalizedWebSocket(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) return
        add(normalized)
    }
}
