package com.tps.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<ProductDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 0
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState

    private var keyword: String? = null
    private var category: String? = null
    private var maxPrice: Double? = null

    init { loadProducts() }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currentPage = 0, products = emptyList())
            try {
                val resp = apiService.getProducts(page = 0, keyword = keyword, category = category, maxPrice = maxPrice)
                val page = resp.data
                if (page != null) {
                    _uiState.value = _uiState.value.copy(
                        products = page.content,
                        hasMore = page.number + 1 < page.totalPages,
                        currentPage = 0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return
        viewModelScope.launch {
            val nextPage = _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val resp = apiService.getProducts(page = nextPage, keyword = keyword, category = category, maxPrice = maxPrice)
                val page = resp.data
                if (page != null) {
                    _uiState.value = _uiState.value.copy(
                        products = _uiState.value.products + page.content,
                        hasMore = page.number + 1 < page.totalPages,
                        currentPage = nextPage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun search(query: String) {
        keyword = query.ifBlank { null }
        maxPrice = null
        loadProducts()
    }

    fun filterCategory(cat: String?) {
        category = cat
        maxPrice = null
        loadProducts()
    }

    fun filterLowPrice() {
        keyword = null
        category = null
        maxPrice = 50.0
        loadProducts()
    }
}
