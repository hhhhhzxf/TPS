package com.tps.data.remote

/**
 * 文件说明：网络地址配置中心，负责整理主地址与备用地址列表供 HTTP 和 WebSocket 共用。
 */

import com.tps.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object NetworkEndpointConfig {
    // 真机调试时只使用构建脚本传入的主地址。自动切换到其他后端会让同一个 token
    // 打到不同数据库/密钥的服务上，表现成管理员页面偶发 401。
    val apiBaseUrls: List<HttpUrl> = listOf(normalizedApiBaseUrl(BuildConfig.BASE_URL))

    val websocketUrls: List<String> = listOf(BuildConfig.WS_URL.trim()).filter { it.isNotEmpty() }

    val primaryApiBaseUrl: HttpUrl = apiBaseUrls.first()

    // 网络层请求成功后会回写该值，用于记录当前这台真机真正打通的是哪一个入口地址。
    @Volatile
    var lastWorkingApiBaseUrl: HttpUrl = primaryApiBaseUrl

    private fun normalizedApiBaseUrl(value: String): HttpUrl =
        value.trim().trimEnd('/').plus("/").toHttpUrl()

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
