package com.tps.ui.auth

/**
 * 文件说明：认证页面状态管理，负责登录注册流程与接口交互。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.LoginRequest
import com.tps.data.remote.dto.RegisterRequest
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun sendCode(phone: String) {
        viewModelScope.launch {
            try {
                apiService.sendCode(phone)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                tokenManager.clear()
                val resp = apiService.login(LoginRequest(phone = phone.trim(), password = password.trim()))
                if (resp.code == 200 && resp.data != null) {
                    tokenManager.saveToken(resp.data.token)
                    tokenManager.saveUserId(resp.data.userId)
                    tokenManager.saveRole(resp.data.role)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        isAdmin = resp.data.role == "ADMIN"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = resp.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun register(phone: String, password: String, code: String, studentId: String, nickname: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val resp = apiService.register(RegisterRequest(phone, password, code, studentId, nickname))
                if (resp.code == 200 && resp.data != null) {
                    tokenManager.saveToken(resp.data.token)
                    tokenManager.saveUserId(resp.data.userId)
                    tokenManager.saveRole(resp.data.role)
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = resp.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
