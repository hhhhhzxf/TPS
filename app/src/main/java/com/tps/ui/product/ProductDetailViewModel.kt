package com.tps.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductDto
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    val deleted: Boolean = false
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
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
                
                try {
                    if (!isOwner) apiService.recordHistory(productId)
                } catch (_: Exception) {}
                
                _uiState.value = _uiState.value.copy(
                    product = product,
                    isLoading = false,
                    isFavorite = product?.favorited == true,
                    isOwner = isOwner
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
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
            } catch (e: Exception) { }
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
            } catch (e: Exception) { }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                apiService.updateProductStatus(productId, "OFF")
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun bumpProduct(productId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.bumpProduct(productId)
                _uiState.value = _uiState.value.copy(product = resp.data ?: _uiState.value.product)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
