package com.tps.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.OrderDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderUiState(
    val orders: List<OrderDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val role: String = "buyer"
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState

    init { loadOrders() }

    fun loadOrders(role: String = _uiState.value.role) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, role = role)
            try {
                val resp = apiService.getMyOrders(role = role)
                _uiState.value = _uiState.value.copy(orders = resp.data?.content ?: emptyList(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun switchRole(role: String) {
        if (role != _uiState.value.role) loadOrders(role)
    }

    fun pay(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.payOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun confirmReceived(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.confirmOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun ship(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.shipOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun cancel(orderId: Long) {
        viewModelScope.launch {
            try {
                apiService.cancelOrder(orderId)
                loadOrders()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
