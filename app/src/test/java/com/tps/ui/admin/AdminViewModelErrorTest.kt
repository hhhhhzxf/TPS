package com.tps.ui.admin

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AdminViewModelErrorTest {

    @Test
    fun adminErrorMessageShowsRefreshFailureFor401() {
        val error = HttpException(Response.error<Unit>(401, "".toResponseBody(null)))

        val message = adminErrorMessage("加载商品", error)

        assertEquals("加载商品失败：登录状态刷新失败，请检查网络后重试", message)
    }

    @Test
    fun adminErrorMessageMaps400ToReadableBusinessError() {
        val error = HttpException(Response.error<Unit>(400, "".toResponseBody(null)))

        val message = adminErrorMessage("更新用户状态", error)

        assertEquals("更新用户状态失败：请求参数或账号状态异常，请检查后重试", message)
    }

    @Test
    fun adminErrorMessageKeepsActionForOtherErrors() {
        val message = adminErrorMessage("加载统计", IllegalStateException("boom"))

        assertEquals("加载统计失败：boom", message)
    }
}
