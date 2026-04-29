package com.tps.data.remote.websocket

import com.google.gson.Gson
import com.tps.data.remote.NetworkEndpointConfig
import com.tps.util.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val senderId: Long = 0,
    val content: String = "",
    val type: String = "TEXT",
    val createdAt: String = ""
)

@Singleton
class StompClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ChatMessage> = _messages

    private var subscribedConversationId: Long = -1
    private var connected = false
    private var pendingSubscription: Long? = null
    private var nextEndpointIndex = 0

    fun connect() {
        if (connected) return
        val token = tokenManager.getToken() ?: return
        val endpoints = NetworkEndpointConfig.websocketUrls
        if (endpoints.isEmpty()) return
        val endpointIndex = nextEndpointIndex.coerceIn(0, endpoints.lastIndex)
        val request = Request.Builder()
            .url(endpoints[endpointIndex])
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                connected = true
                // STOMP CONNECT frame
                ws.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\u0000")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                when {
                    text.startsWith("CONNECTED") -> {
                        pendingSubscription?.let { subscribeConversation(it) }
                    }
                    text.startsWith("MESSAGE") -> {
                        val body = text.substringAfterLast("\n\n").trimEnd('\u0000')
                        try {
                            val msg = gson.fromJson(body, ChatMessage::class.java)
                            _messages.tryEmit(msg)
                        } catch (_: Exception) {}
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                connected = false
                webSocket = null
                if (endpointIndex < endpoints.lastIndex) {
                    nextEndpointIndex = endpointIndex + 1
                    connect()
                } else {
                    nextEndpointIndex = 0
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                connected = false
            }
        })
    }

    fun subscribeConversation(conversationId: Long) {
        subscribedConversationId = conversationId
        if (!connected) {
            pendingSubscription = conversationId
            connect()
            return
        }
        webSocket?.send(
            "SUBSCRIBE\nid:sub-$conversationId\ndestination:/topic/conversation/$conversationId\n\n\u0000"
        )
    }

    fun sendMessage(conversationId: Long, senderId: Long, content: String) {
        val payload = gson.toJson(mapOf(
            "conversationId" to conversationId,
            "senderId" to senderId,
            "content" to content,
            "type" to "TEXT"
        ))
        val frame = "SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n$payload\u0000"
        webSocket?.send(frame)
    }

    fun disconnect() {
        webSocket?.send("DISCONNECT\n\n\u0000")
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        connected = false
    }
}
