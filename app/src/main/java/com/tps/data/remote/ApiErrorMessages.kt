package com.tps.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException

fun apiErrorMessage(error: Exception): String? {
    if (error !is HttpException) return null
    val body = error.response()?.errorBody()?.string() ?: return null
    return runCatching {
        JsonParser.parseString(body).asJsonObject.get("message")?.asString
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
