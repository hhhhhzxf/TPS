package com.tps.util

import com.tps.BuildConfig

fun resolveMediaUrl(url: String?): String? {
    val value = url?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("content://")) {
        return value
    }
    val base = BuildConfig.BASE_URL.trimEnd('/')
    return if (value.startsWith("/")) "$base$value" else "$base/$value"
}
