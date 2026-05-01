package com.example.chat_app_android.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.models.DeviceTokenRequest
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var loginSucceeded by mutableStateOf(false)
        private set

    fun clearSuccessMessage() {
        successMessage = null
    }

    fun consumeLoginSuccess() {
        loginSucceeded = false
    }

    fun loginUser(email: String, password: String, context: Context) {
        viewModelScope.launch {
            val sessionManager = SessionManager(context)
            isLoading = true
            errorMessage = null

            try {
                val request = AuthRequest(email = email, password = password)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful) {
                    val authBody = response.body()

                    if (authBody?.token != null) {
                        sessionManager.saveAuthToken(authBody.token)
                        sessionManager.saveEmail(email)
                        successMessage = authBody.message

                        val meResponse = RetrofitClient.apiService.getMe("Bearer ${authBody.token}")
                        if (meResponse.isSuccessful) {
                            meResponse.body()?.let {
                                sessionManager.saveUserId(it.id)
                                sessionManager.saveUsername(it.username)
                            }
                        }

                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    return@addOnCompleteListener
                                }

                                val fcmToken = task.result
                                viewModelScope.launch {
                                    try {
                                        RetrofitClient.apiService.saveDeviceToken(
                                            token = "Bearer ${authBody.token}",
                                            request = DeviceTokenRequest(fcmToken)
                                        )
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    }
                                }
                            }

                        loginSucceeded = true
                    }
                } else {
                    val errorJson = response.errorBody()?.string()
                    val errorObj = Gson().fromJson(errorJson, AuthResponse::class.java)
                    errorMessage = errorObj?.message ?: "Невалидни данни"
                }
            } catch (e: Exception) {
                errorMessage = "Лоша връзка с мрежата: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}