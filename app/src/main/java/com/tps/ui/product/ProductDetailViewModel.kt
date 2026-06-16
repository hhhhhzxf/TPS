package com.tps.ui.product

/**
 * 文件说明：商品模块状态管理，负责商品列表、详情、发布与状态流转的数据编排。
 */

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductCommentDto
import com.tps.data.remote.dto.ProductCommentRequest
import com.tps.data.remote.dto.ProductDto
import com.tps.data.remote.dto.ReportProductRequest
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class ProductDetailUiState(
    val product: ProductDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val orderCreated: Boolean = false,
    val orderError: String? = null,
    val navigateToChatId: Long? = null,
    val isOwner: Boolean = false,
    val deleted: Boolean = false,
    val actionSuccess: String? = null,
    val comments: List<ProductCommentDto> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentSubmitting: Boolean = false,
    val reportSubmitted: Boolean = false,
    val isReporting: Boolean = false
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    fun load(productId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val resp = apiService.getProduct(productId)
                val product = resp.data
                val isOwner = product?.userId == tokenManager.getUserId()
                _uiState.value = _uiState.value.copy(
                    product = product,
                    isLoading = false,
                    isFavorite = product?.favorited == true,
                    isOwner = isOwner,
                    comments = emptyList(),
                    commentsLoading = product != null
                )
                product?.let { loadComments(it.id, showError = true) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun refreshComments() {
        val productId = _uiState.value.product?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentsLoading = true)
            loadComments(productId, showError = true)
        }
    }

    fun submitComment(content: String) {
        val productId = _uiState.value.product?.id ?: return
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入评论内容")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentSubmitting = true)
            try {
                val resp = apiService.createProductComment(
                    productId,
                    ProductCommentRequest(content = trimmed.take(500))
                )
                val createdComment = resp.data
                _uiState.value = if (createdComment != null) {
                    _uiState.value.copy(
                        comments = listOf(createdComment) + _uiState.value.comments.filter { it.id != createdComment.id },
                        commentSubmitting = false,
                        actionSuccess = "评论已发布"
                    )
                } else {
                    _uiState.value.copy(commentSubmitting = false, error = resp.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    commentSubmitting = false,
                    error = commentErrorMessage(e, "评论发布失败")
                )
            }
        }
    }

    fun deleteComment(commentId: Long) {
        val productId = _uiState.value.product?.id ?: return
        viewModelScope.launch {
            try {
                apiService.deleteProductComment(productId, commentId)
                _uiState.value = _uiState.value.copy(
                    comments = _uiState.value.comments.filterNot { it.id == commentId },
                    actionSuccess = "评论已删除"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = commentErrorMessage(e, "删除评论失败"))
            }
        }
    }

    private suspend fun loadComments(productId: Long, showError: Boolean) {
        try {
            val resp = apiService.getProductComments(productId, page = 0, size = 20)
            _uiState.value = _uiState.value.copy(
                comments = resp.data?.content ?: emptyList(),
                commentsLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                commentsLoading = false,
                error = if (showError) "评论加载失败" else _uiState.value.error
            )
        }
    }

    private fun commentErrorMessage(error: Exception, fallback: String): String {
        return if (error is HttpException && error.code() == 401) {
            "请先登录后再评论"
        } else {
            error.message ?: fallback
        }
    }

    fun createOrder(productId: Long) {
        viewModelScope.launch {
            try {
                val price = _uiState.value.product?.price ?: 0.0
                apiService.createOrder(productId = productId, finalPrice = price)
                _uiState.value = _uiState.value.copy(orderCreated = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(orderError = e.message)
            }
        }
    }

    fun startChat(sellerId: Long, productId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.getOrCreateConversation(targetUserId = sellerId, productId = productId)
                resp.data?.let { _uiState.value = _uiState.value.copy(navigateToChatId = it.id) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "无法创建会话")
            }
        }
    }

    fun consumeNavigateToChat() {
        _uiState.value = _uiState.value.copy(navigateToChatId = null)
    }

    fun toggleFavorite(productId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.toggleFavorite(productId)
                _uiState.value = _uiState.value.copy(isFavorite = resp.data == true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "收藏操作失败")
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                apiService.updateProductStatus(productId, "OFF")
                _uiState.value = _uiState.value.copy(deleted = true, actionSuccess = "商品已下架")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateStatus(productId: Long, status: String) {
        viewModelScope.launch {
            try {
                val resp = apiService.updateProductStatus(productId, status)
                _uiState.value = _uiState.value.copy(
                    product = resp.data ?: _uiState.value.product,
                    actionSuccess = if (status == "ON_SALE") "商品已重新上架" else "商品已下架"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun bumpProduct(productId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.bumpProduct(productId)
                _uiState.value = _uiState.value.copy(product = resp.data ?: _uiState.value.product, actionSuccess = "商品已擦亮")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun reportProduct(productId: Long, reason: String, evidenceUris: List<Uri>) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请填写举报原因")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReporting = true, error = null, reportSubmitted = false)
            try {
                val evidenceUrls = evidenceUris.mapNotNull { uploadEvidenceImage(it) }
                val resp = apiService.reportProduct(
                    productId,
                    ReportProductRequest(reason = reason.trim(), evidenceImageUrls = evidenceUrls)
                )
                if (resp.code == 200) {
                    _uiState.value = _uiState.value.copy(isReporting = false, reportSubmitted = true, actionSuccess = "举报已提交，平台将进行审核")
                } else {
                    _uiState.value = _uiState.value.copy(isReporting = false, error = resp.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isReporting = false, error = e.message ?: "举报失败")
            }
        }
    }

    fun consumeActionSuccess() {
        _uiState.value = _uiState.value.copy(actionSuccess = null)
    }

    fun consumeReportSubmitted() {
        _uiState.value = _uiState.value.copy(reportSubmitted = false)
    }

    private suspend fun uploadEvidenceImage(uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", resolveFileName(uri), body)
        return apiService.uploadImage(part).data?.url
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrBlank()) return name
            }
        }
        return "report-evidence.jpg"
    }
}
