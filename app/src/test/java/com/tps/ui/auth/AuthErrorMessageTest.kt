package com.tps.ui.auth

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthErrorMessageTest {

    @Test
    fun loginErrorMessageUsesBackendMessageWhenAvailable() {
        val body = """{"code":400,"message":"账号被封禁，请联系管理员","data":null}"""
        val error = HttpException(Response.error<Unit>(400, body.toResponseBody(null)))

        assertEquals("账号被封禁，请联系管理员", loginErrorMessage(error))
    }

    @Test
    fun loginErrorMessageMaps400ToBannedAccountFallback() {
        val error = HttpException(Response.error<Unit>(400, "".toResponseBody(null)))

        assertEquals("账号被封禁，请联系管理员", loginErrorMessage(error))
    }

    @Test
    fun loginErrorMessageMaps401ToExpiredSessionMessage() {
        val error = HttpException(Response.error<Unit>(401, "".toResponseBody(null)))

        assertEquals("登录状态已过期，请重新登录", loginErrorMessage(error))
    }
}
