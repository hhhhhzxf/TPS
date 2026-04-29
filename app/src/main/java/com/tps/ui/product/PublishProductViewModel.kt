package com.tps.ui.product

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.dto.ProductRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class PublishUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val selectedImages: List<Uri> = emptyList()
)

@HiltViewModel
class PublishProductViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState

    fun addImages(uris: List<Uri>) {
        val current = _uiState.value.selectedImages.toMutableList()
        current.addAll(uris.take(9 - current.size))
        _uiState.value = _uiState.value.copy(selectedImages = current)
    }

    fun removeImage(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImages = _uiState.value.selectedImages.filter { it != uri }
        )
    }

    fun publish(title: String, description: String, price: Double, category: String, condition: String, location: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val imageUrls = _uiState.value.selectedImages.mapNotNull { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@mapNotNull null
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", resolveFileName(uri), body)
                    val response = apiService.uploadImage(part)
                    response.data?.url
                }
                val req = ProductRequest(title, description, price, category, condition, location, imageUrls)
                val response = apiService.createProduct(req)
                if (response.code == 200 && response.data != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = response.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrBlank()) return name
            }
        }
        return "image.jpg"
    }
}
