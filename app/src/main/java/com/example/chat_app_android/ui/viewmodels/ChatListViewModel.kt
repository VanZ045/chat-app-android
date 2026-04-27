package com.example.chat_app_android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.ChatSummaryModel
import com.example.chat_app_android.data.models.ChatUpdateEvent
import com.example.chat_app_android.data.models.TypingRequest
import com.example.chat_app_android.data.models.UserModel
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatListViewModel(application: Application) : AndroidViewModel(application){

    private val sessionManager = SessionManager(application)
    private val gson = Gson()
    private var stompSession: StompSession? = null

    private val subscribedTypingChatIds = mutableSetOf<Long>()

    private val _chats = MutableStateFlow<List<ChatSummaryModel>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _users = MutableStateFlow<List<UserModel>>(emptyList())
    val users = _users.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _sessionExpired = MutableStateFlow<Boolean>(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    private val _navigateToChat = MutableStateFlow<ChatSummaryModel?>(null)
    val navigateToChat = _navigateToChat.asStateFlow()

    private val _typingChats = MutableStateFlow<Set<Long>>(emptySet())
    val typingChats = _typingChats.asStateFlow()

    fun loadUsers(){
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }

        viewModelScope.launch{
            _isLoading.value = true
            _error.value = null
            try {
                val usersResponse = RetrofitClient.apiService.getAllUsers("Bearer $token")
                val chatsResponse = RetrofitClient.apiService.getChats("Bearer $token")

                if (usersResponse.code() == 401 || chatsResponse.code() == 401) {
                    sessionManager.clearSession()
                    _sessionExpired.value = true
                    return@launch
                }

                if (usersResponse.isSuccessful) {
                    _users.value = usersResponse.body() ?: emptyList()
                } else {
                    _error.value = "Неуспешно зареждане на потребители"
                }

                if (chatsResponse.isSuccessful) {
                    _chats.value = chatsResponse.body() ?: emptyList()
                } else {
                    _error.value = "Неуспешно зареждане на чатове"
                }
            } catch (e: Exception) {
                _error.value = "Мрежова грешка"
            } finally {
                _isLoading.value = false
            }
            connectToUserUpdates()
        }
    }

    private fun connectToUserUpdates(){
        val userId = sessionManager.fetchUserId()
        if(userId == -1L)return

        viewModelScope.launch {
            try{
                val client = StompClient(OkHttpWebSocketClient())
                // for emulator use ws://10.0.2.2:8080/ws
                // for phone use ws://192.168.1.15:8080/ws
                stompSession = client.connect("ws://192.168.0.7:8080/ws")

                launch{
                    stompSession!!.subscribeText("/topic/user/$userId")
                        .collect{frame ->
                            try{
                                val event = gson.fromJson(frame, ChatUpdateEvent::class.java)
                                val updated = _chats.value.map{chat ->
                                    if(chat.chatId == event.chatId){
                                        chat.copy(
                                            lastMessage = event.lastMessage,
                                            lastMessageTime = event.lastMessageTime,
                                            lastMessageSenderId = event.lastMessageSenderId
                                        )
                                    }else chat
                                }.sortedByDescending { it.lastMessageTime }
                                _chats.value = updated
                            }catch(e: Exception){}
                        }
                }
                subscribeTypingForCurrentChats()
            }catch(e: Exception){}
        }
    }

    private fun subscribeTypingForCurrentChats(){
        val userId = sessionManager.fetchUserId()

        _chats.value.forEach { chat ->
            if(subscribedTypingChatIds.contains(chat.chatId)) return@forEach
            subscribedTypingChatIds.add(chat.chatId)
            viewModelScope.launch {
                try{
                    stompSession!!.subscribeText("/topic/chats/${chat.chatId}/typing")
                        .collect { frame ->
                            try{
                                val event = gson.fromJson(frame, TypingRequest::class.java)
                                if(event.senderId != userId){
                                    if(event.isTyping){
                                        _typingChats.value += chat.chatId
                                    }else{
                                        _typingChats.value -= chat.chatId
                                    }
                                }
                            }catch (e: Exception){}
                        }
                }catch (e: Exception){}
            }
        }
    }

    fun openOrCreateChat(otherUserId: Long){
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.createOrGetChat("Bearer $token", otherUserId)
                when(response.code()){
                    200 -> {
                        val chat = response.body()
                        if(chat != null){
                            if(_chats.value.none{it.chatId == chat.chatId}){
                                _chats.value += chat
                                subscribeTypingForCurrentChats()
                            }
                            _navigateToChat.value = chat
                        }
                    }
                    401 -> {sessionManager.clearSession(); _sessionExpired.value = true}
                    else -> _error.value = "Неуспешно зареждане на чат"
                }
            }catch(e: Exception){
                _error.value = "Неуспешно зареждане на чат"
            }
        }
    }

    fun getCurrentUserId(): Long = sessionManager.fetchUserId()

    fun clearNavigation() {_navigateToChat.value = null}

    fun formatTime(timestamp: String?): String{
        return try{
            val dt = LocalDateTime.parse(timestamp)
            val now = LocalDateTime.now()
            when{
                dt.toLocalDate() == now.toLocalDate() ->
                    dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                dt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Вчера"
                else ->
                    dt.format(DateTimeFormatter.ofPattern("dd/MM"))
            }
        }catch (e: Exception) {""}
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { stompSession?.disconnect() }
    }

    fun deleteChat(chatId: Long){
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }
        viewModelScope.launch {
            try{
                val response = RetrofitClient.apiService.deleteChat("Bearer $token", chatId)
                if(response.code() == 200){
                    _chats.value = _chats.value.filter { it.chatId != chatId }
                }else if(response.code() == 401){
                    sessionManager.clearSession()
                    _sessionExpired.value = true
                }
            }catch (e: Exception){
                _error.value = "Неуспешно изтриване на чат"
            }
        }
    }
}