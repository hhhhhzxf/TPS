package com.tps.ui.admin

/**
 * 文件说明：管理员页面状态管理，负责后台管理数据加载与操作编排。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.AdminStats
import com.tps.data.remote.dto.FeedbackDto
import com.tps.data.remote.dto.FeedbackReplyRequest
import com.tps.data.remote.dto.OrderDto
import com.tps.data.remote.dto.ProductDto
import com.tps.data.remote.dto.ReportDto
import com.tps.data.remote.dto.UserProfile
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class AdminUiState(
    val users: List<UserProfile> = emptyList(),
    val userKeyword: String = "",
    val userStatus: String? = null,
    val userSort: String = "createdAt",
    val userDirection: String = "desc",
    val listedProducts: List<ProductDto> = emptyList(),
    val reportedProducts: List<ReportDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val feedback: List<FeedbackDto> = emptyList(),
    val stats: AdminStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val sessionExpired: Boolean = false,
    val operatingProductId: Long? = null,
    val operatingOrderId: Long? = null,
    val operatingFeedbackId: Long? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    fun loadUsers(
        keyword: String = _uiState.value.userKeyword,
        status: String? = _uiState.value.userStatus,
        sort: String = _uiState.value.userSort,
        direction: String = _uiState.value.userDirection
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val normalizedKeyword = keyword.trim()
                val resp = apiService.adminGetUsers(
                    status = status,
                    keyword = normalizedKeyword.ifBlank { null },
                    sort = sort,
                    direction = direction
                )
                _uiState.value = _uiState.value.copy(
                    users = resp.data?.content ?: emptyList(),
                    userKeyword = keyword,
                    userStatus = status,
                    userSort = sort,
                    userDirection = direction
                )
            } catch (e: Exception) {
                setAdminError("加载用户", e)
            }
        }
    }

    fun loadListedProducts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val resp = apiService.adminGetProducts(status = "ON_SALE")
                _uiState.value = _uiState.value.copy(listedProducts = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                setAdminError("加载商品", e)
            }
        }
    }

    fun loadReportedProducts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val resp = apiService.adminGetReports()
                _uiState.value = _uiState.value.copy(reportedProducts = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                setAdminError("加载举报", e)
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val resp = apiService.adminGetOrders()
                _uiState.value = _uiState.value.copy(orders = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                setAdminError("加载订单", e)
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val resp = apiService.adminGetStats()
                _uiState.value = _uiState.value.copy(stats = resp.data)
            } catch (e: Exception) {
                setAdminError("加载统计", e)
            }
        }
    }

    fun loadFeedback(status: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                val resp = apiService.adminGetFeedback(status = status)
                _uiState.value = _uiState.value.copy(feedback = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                setAdminError("加载反馈", e)
            }
        }
    }

    fun banUser(userId: Long, isBanned: Boolean) {
        viewModelScope.launch {
            try {
                if (isBanned) apiService.adminUnbanUser(userId)
                else apiService.adminBanUser(userId)
                loadUsers()
            } catch (e: Exception) {
                setAdminError("更新用户状态", e)
            }
        }
    }

    fun handleReport(reportId: Long, takedown: Boolean, reason: String? = null) {
        if (reason.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = if (takedown) "请填写下架原因" else "请填写驳回原因")
            return
        }
        viewModelScope.launch {
            try {
                apiService.adminHandleReport(reportId, takedown, reason?.trim())
                _uiState.value = _uiState.value.copy(successMessage = if (takedown) "举报已处理，商品已下架" else "举报已驳回")
                loadReportedProducts()
                loadListedProducts()
            } catch (e: Exception) {
                setAdminError("处理举报", e)
            }
        }
    }

    fun takedownProduct(productId: Long, reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请填写下架原因")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operatingProductId = productId,
                error = null,
                successMessage = null
            )
            try {
                apiService.adminTakedownProduct(productId, reason.trim())
                _uiState.value = _uiState.value.copy(
                    successMessage = "商品已强制下架",
                    operatingProductId = null
                )
                loadListedProducts()
                loadStats()
            } catch (e: Exception) {
                setAdminError("下架商品", e, clearProductOperation = true)
            }
        }
    }

    fun approveRefund(orderId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operatingOrderId = orderId,
                error = null,
                successMessage = null
            )
            try {
                apiService.adminApproveRefund(orderId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "退款已通过",
                    operatingOrderId = null
                )
                loadOrders()
                loadStats()
            } catch (e: Exception) {
                setAdminError("通过退款", e, clearOrderOperation = true)
            }
        }
    }

    fun rejectRefund(orderId: Long, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operatingOrderId = orderId,
                error = null,
                successMessage = null
            )
            try {
                apiService.adminRejectRefund(orderId, reason.trim().ifBlank { null })
                _uiState.value = _uiState.value.copy(
                    successMessage = "退款已驳回",
                    operatingOrderId = null
                )
                loadOrders()
                loadStats()
            } catch (e: Exception) {
                setAdminError("驳回退款", e, clearOrderOperation = true)
            }
        }
    }

    fun replyFeedback(feedbackId: Long, reply: String) {
        if (reply.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请填写回复内容")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operatingFeedbackId = feedbackId,
                error = null,
                successMessage = null
            )
            try {
                apiService.adminReplyFeedback(feedbackId, FeedbackReplyRequest(reply.trim()))
                _uiState.value = _uiState.value.copy(
                    successMessage = "反馈已回复",
                    operatingFeedbackId = null
                )
                loadFeedback()
            } catch (e: Exception) {
                setAdminError("回复反馈", e, clearFeedbackOperation = true)
            }
        }
    }

    fun updateFeedbackStatus(feedbackId: Long, status: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                operatingFeedbackId = feedbackId,
                error = null,
                successMessage = null
            )
            try {
                apiService.adminUpdateFeedbackStatus(feedbackId, status)
                _uiState.value = _uiState.value.copy(
                    successMessage = "反馈状态已更新",
                    operatingFeedbackId = null
                )
                loadFeedback()
            } catch (e: Exception) {
                setAdminError("更新反馈状态", e, clearFeedbackOperation = true)
            }
        }
    }

    fun consumeMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun setAdminError(
        action: String,
        error: Exception,
        clearProductOperation: Boolean = false,
        clearOrderOperation: Boolean = false,
        clearFeedbackOperation: Boolean = false
    ) {
        if (isAdminSessionExpired(error)) {
            tokenManager.clear()
        }
        _uiState.value = _uiState.value.copy(
            error = adminErrorMessage(action, error),
            sessionExpired = isAdminSessionExpired(error),
            operatingProductId = if (clearProductOperation) null else _uiState.value.operatingProductId,
            operatingOrderId = if (clearOrderOperation) null else _uiState.value.operatingOrderId,
            operatingFeedbackId = if (clearFeedbackOperation) null else _uiState.value.operatingFeedbackId
        )
    }
}

internal fun isAdminSessionExpired(error: Exception): Boolean =
    error is HttpException && error.code() == 401

internal fun adminErrorMessage(action: String, error: Exception): String {
    val detail = when {
        isAdminSessionExpired(error) -> "管理员登录已过期，请重新登录"
        error is HttpException && error.code() == 403 -> "当前账号没有管理员权限"
        error is HttpException -> "HTTP ${error.code()}"
        !error.message.isNullOrBlank() -> error.message!!
        else -> "网络请求失败"
    }
    return "${action}失败：$detail"
}
