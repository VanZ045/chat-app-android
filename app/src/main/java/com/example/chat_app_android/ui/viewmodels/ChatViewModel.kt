package com.example.chat_app_android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.MessageModel
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
    private var currentEmail: String = ""

    fun getCurrentUserEmail(): String = sessionManager.fetchEmail() ?: ""

    fun loadMessages(receiverEmail: String) {
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }
        currentEmail = getCurrentUserEmail()

        viewModelScope.launch {
            _isLoading.value = true
            try{
                val response = RetrofitClient.apiService.getConversation(
                    token = "Bearer $token",
                    email1 = currentEmail,
                    email2 = receiverEmail
                )
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
            connectWebSocket()
        }
    }

    private fun connectWebSocket(){
        viewModelScope.launch {
            try{
                val client = StompClient(OkHttpWebSocketClient())
                stompSession = client.connect("ws://10.0.2.2:8080/ws")

                stompSession!!.subscribeText("/topic/messages/$currentEmail")
                    .collect{frame ->
                        try {
                            val incoming = gson.fromJson(frame, IncomingMessage::class.java)
                            val newMessage = MessageModel(
                                id = System.currentTimeMillis(),
                                senderEmail = incoming.senderEmail ?: "",
                                receiverEmail = incoming.receiverEmail ?: "",
                                content = incoming.content ?: "",
                                timestamp = ""
                            )
                            _messages.value += newMessage
                        }catch(e: Exception){}
                    }
            }catch(e: Exception){
                _error.value = "Real-time connection failed - messages may be delayed"
            }
        }
    }

    fun sendMessage(receiverEmail: String, content: String){
        if(content.isBlank()) return

        viewModelScope.launch {
            try{
                val message = mapOf(
                    "senderEmail" to currentEmail,
                    "receiverEmail" to receiverEmail,
                    "content" to content
                )
                stompSession?.sendText(
                    "app/chat.send",
                    gson.toJson(message)
                )
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

    private data class IncomingMessage(
        val senderEmail: String? = null,
        val receiverEmail: String? = null,
        val content: String? = null
    )
}