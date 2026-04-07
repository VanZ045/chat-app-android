package com.example.chat_app_android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.MessageModel
import com.example.chat_app_android.data.models.SendMessageRequest
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val gson = Gson()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    private var stompSession: StompSession? = null

    fun getCurrentUserId(): Long = sessionManager.fetchUserId()

    fun loadMessages(chatId: Long) {
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try{
                val response = RetrofitClient.apiService.getMessages(token = "Bearer $token", chatId)
                when(response.code()){
                    200 -> _messages.value = response.body() ?: emptyList()
                    401 -> {
                        sessionManager.clearSession()
                        _sessionExpired.value = true
                        return@launch
                    }
                    else -> _error.value = "Failed to load messages"
                }
            }catch(e: Exception){
                _error.value = "Network error"
            } finally {
                _isLoading.value = false
            }
            connectWebSocket(chatId)
        }
    }

    private fun connectWebSocket(chatId: Long){
        viewModelScope.launch {
            try{
                val client = StompClient(OkHttpWebSocketClient())
                // for emulator use ws://10.0.2.2:8080/ws
                stompSession = client.connect("ws://192.168.0.x:8080/ws")

                stompSession!!.subscribeText("/topic/chats/$chatId")
                    .collect{frame ->
                        try {
                            val incoming = gson.fromJson(frame, MessageModel::class.java)
                            if(_messages.value.none {it.id == incoming.id}){
                                _messages.value += incoming
                            }
                        }catch(e: Exception){}
                    }
            }catch(e: Exception){
                _error.value = "Real-time connection failed"
            }
        }
    }

    fun sendMessage(chatId: Long, content: String){
        if(content.isBlank()) return
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }

        viewModelScope.launch {
            try{
                val response = RetrofitClient.apiService.sendMessage(
                    "Bearer $token",
                    chatId,
                    SendMessageRequest(content)
                )
                when(response.code()){
                    200 -> {}
                    401 -> {sessionManager.clearSession(); _sessionExpired.value = true}
                    else -> _error.value = "Failed to send message"
                }
            }catch(e: Exception){
                _error.value = "Failed to send message"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stompSession?.disconnect()
        }
    }
}