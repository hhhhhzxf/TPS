package com.tps.ui.admin

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AdminViewModelErrorTest {

    @Test
    fun adminErrorMessageShowsExpiredLoginFor401() {
        val error = HttpException(Response.error<Unit>(401, "".toResponseBody(null)))

        val message = adminErrorMessage("加载商品", error)

        assertEquals("加载商品失败：管理员登录已过期，请重新登录", message)
    }

    @Test
    fun isAdminSessionExpiredOnlyMatches401() {
        val unauthorized = HttpException(Response.error<Unit>(401, "".toResponseBody(null)))
        val forbidden = HttpException(Response.error<Unit>(403, "".toResponseBody(null)))

        assertEquals(true, isAdminSessionExpired(unauthorized))
        assertEquals(false, isAdminSessionExpired(forbidden))
        assertEquals(false, isAdminSessionExpired(IllegalStateException("boom")))
    }

    @Test
    fun adminErrorMessageKeepsActionForOtherErrors() {
        val message = adminErrorMessage("加载统计", IllegalStateException("boom"))

        assertEquals("加载统计失败：boom", message)
    }
}
