package com.example.chat_app_android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.ChatSummaryModel
import com.example.chat_app_android.data.models.UserModel
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatListViewModel(application: Application) : AndroidViewModel(application){

    private val sessionManager = SessionManager(application)

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

                if (usersResponse.isSuccessful && chatsResponse.isSuccessful) {
                    val userList = usersResponse.body() ?: emptyList()
                    val chatList = chatsResponse.body() ?: emptyList()

                    _users.value = userList
                    _chats.value = chatList
                } else if (usersResponse.code() == 401 || chatsResponse.code() == 401) {
                    sessionManager.clearSession()
                    _sessionExpired.value = true
                } else {
                    _error.value = "Server error: ${usersResponse.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
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
                    200 -> _navigateToChat.value = response.body()
                    401 -> {sessionManager.clearSession(); _sessionExpired.value = true}
                    else -> _error.value = "Failed to open chat"
                }
            }catch(e: Exception){
                _error.value = "Failed to open chat"
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
                dt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
                else ->
                    dt.format(DateTimeFormatter.ofPattern("dd/MM"))
            }
        }catch (e: Exception) {""}
    }
}