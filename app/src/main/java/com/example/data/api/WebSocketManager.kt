package com.example.data.api

import com.example.data.model.ModelsDemand
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import org.json.JSONObject

sealed class WebSocketState {
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    object Disconnected : WebSocketState()
    object MockMode : WebSocketState()
}

sealed class WebSocketEvent {
    data class DemandCreated(val demand: ModelsDemand) : WebSocketEvent()
    data class DemandUpdated(val demand: ModelsDemand) : WebSocketEvent()
    data class DemandDeleted(val id: String) : WebSocketEvent()
    object GenericEvent : WebSocketEvent()
}

class WebSocketManager {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    
    private val _state = MutableSharedFlow<WebSocketState>(replay = 1)
    val state: SharedFlow<WebSocketState> = _state

    private val _events = MutableSharedFlow<WebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<WebSocketEvent> = _events

    init {
        _state.tryEmit(WebSocketState.Disconnected)
    }

    fun connect(token: String) {
        val url = "wss://system.tipmp.com.br/api/ws?token=$token"
        val request = Request.Builder().url(url).build()
        _state.tryEmit(WebSocketState.Connecting)

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.tryEmit(WebSocketState.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _events.tryEmit(WebSocketEvent.GenericEvent)
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    val data = json.optJSONObject("data")
                    
                    if (data != null) {
                        val demand = ModelsDemand(
                            id = data.optString("id"),
                            title = data.optString("title"),
                            description = data.optString("description"),
                            priority = data.optString("priority"),
                            status = data.optString("status"),
                            category = data.optString("category"),
                            requesterName = data.optString("requesterName"),
                            createdAt = data.optString("createdAt")
                        )
                        when (type) {
                            "CREATE" -> _events.tryEmit(WebSocketEvent.DemandCreated(demand))
                            "UPDATE" -> _events.tryEmit(WebSocketEvent.DemandUpdated(demand))
                        }
                    } else if (type == "DELETE") {
                        val id = json.optString("id")
                        _events.tryEmit(WebSocketEvent.DemandDeleted(id))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.tryEmit(WebSocketState.Disconnected)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _state.tryEmit(WebSocketState.Disconnected)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.tryEmit(WebSocketState.Disconnected)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        _state.tryEmit(WebSocketState.Disconnected)
    }

    fun simulateEvent(event: WebSocketEvent) {
        _events.tryEmit(event)
    }

    fun setMockMode() {
        _state.tryEmit(WebSocketState.MockMode)
    }
}
