package com.example.chat_app_android.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.ChatPreview
import com.example.chat_app_android.data.models.UserModel
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatListViewModel(application: Application) : AndroidViewModel(application){

    private val sessionManager = SessionManager(application)

    private val _chats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _sessionExpired = MutableStateFlow<Boolean>(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    fun loadUsers(){
        val token = sessionManager.fetchAuthToken()
        if(token == null){
            _sessionExpired.value = true
            return
        }

        viewModelScope.launch{
            _isLoading.value = true
            try{
                val response = RetrofitClient.apiService.getAllUsers("Bearer $token")
                if(response.code() == 401){
                    sessionManager.clearSession()
                    _sessionExpired.value = true
                    return@launch
                }
                val users = response.body() ?: emptyList()
                val currentEmail = getCurrentUserEmail()

                val previews = users
                    .filter{it.email != currentEmail}
                    .map{user ->
                        async{
                            val lastMsgResponse = try{
                                RetrofitClient.apiService.getLastMessage(
                                    token = "Bearer $token",
                                    email1 = currentEmail,
                                    email2 = user.email
                                )
                            }catch(e: Exception) {null}
                            val lastMsg = lastMsgResponse?.body()
                            ChatPreview(
                                user = user,
                                lastMessage = lastMsg?.content ?: "Tap to start chatting",
                                lastMessageTime = if(lastMsg != null) formatTime(lastMsg.timestamp) else "",
                                isMine = lastMsg?.senderEmail == currentEmail
                            )
                        }
                    }.awaitAll()
                _chats.value = previews.sortedByDescending { it.lastMessageTime }
            }catch(e: Exception){
                _error.value = "Network error"
            }finally{
                _isLoading.value = false
            }
        }
    }

    fun getCurrentUserEmail(): String{
        return sessionManager.fetchEmail() ?: ""
    }

    private fun formatTime(timestamp: String): String{
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