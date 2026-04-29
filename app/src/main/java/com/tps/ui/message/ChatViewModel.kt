package com.tps.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tps.data.remote.api.ApiService
import com.tps.data.remote.websocket.ChatMessage
import com.tps.data.remote.websocket.StompClient
import com.tps.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val apiService: ApiService,
    private val stompClient: StompClient,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _title = MutableStateFlow("聊天")
    val title: StateFlow<String> = _title

    private val _product = MutableStateFlow<com.tps.data.remote.dto.ProductDto?>(null)
    val product: StateFlow<com.tps.data.remote.dto.ProductDto?> = _product

    val myUserId: Long get() = tokenManager.getUserId()

    private var conversationId: Long = -1

    fun init(conversationId: Long) {
        this.conversationId = conversationId
        loadHistory()
        loadTitle(conversationId)
        subscribeWebSocket()
    }

    private fun loadTitle(convId: Long) {
        viewModelScope.launch {
            try {
                val resp = apiService.getConversations()
                val conv = resp.data?.content?.find { it.id == convId }
                if (conv != null) {
                    val myId = tokenManager.getUserId()
                    val otherRole = if (conv.buyerId == myId) "卖家" else "买家"
                    _title.value = "商品 #${conv.productId} · $otherRole"

                    val prodResp = apiService.getProduct(conv.productId)
                    _product.value = prodResp.data
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val resp = apiService.getMessages(conversationId)
                val history = resp.data?.map {
                    ChatMessage(
                        id = it.id,
                        conversationId = conversationId,
                        senderId = it.senderId,
                        content = it.content,
                        type = it.type,
                        createdAt = it.createdAt
                    )
                } ?: emptyList()
                _messages.value = history
            } catch (_: Exception) {}
        }
    }

    private fun subscribeWebSocket() {
        stompClient.subscribeConversation(conversationId)
        viewModelScope.launch {
            stompClient.messages
                .filter { it.conversationId == conversationId }
                .collect { msg ->
                    _messages.value = _messages.value + msg
                }
        }
    }

    fun sendMessage(content: String) {
        stompClient.sendMessage(conversationId, myUserId, content)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
