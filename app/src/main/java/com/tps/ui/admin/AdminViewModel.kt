package com.tps.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.AdminStats
import com.tps.data.remote.dto.OrderDto
import com.tps.data.remote.dto.ReportDto
import com.tps.data.remote.dto.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val users: List<UserProfile> = emptyList(),
    val reportedProducts: List<ReportDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val stats: AdminStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    init {
        loadUsers()
        loadReportedProducts()
        loadOrders()
        loadStats()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetUsers()
                _uiState.value = _uiState.value.copy(users = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadReportedProducts() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetReports()
                _uiState.value = _uiState.value.copy(reportedProducts = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetOrders()
                _uiState.value = _uiState.value.copy(orders = resp.data?.content ?: emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val resp = apiService.adminGetStats()
                _uiState.value = _uiState.value.copy(stats = resp.data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
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
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun takeDownProduct(productId: Long) {
        viewModelScope.launch {
            try {
                apiService.adminTakedownProduct(productId)
                loadReportedProducts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
