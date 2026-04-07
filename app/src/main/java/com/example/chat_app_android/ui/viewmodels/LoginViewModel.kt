package com.example.chat_app_android.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)





    fun loginUser(navController: NavController, email: String, password: String , context: Context) {
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
                        if (meResponse.isSuccessful){
                            meResponse.body()?.let {
                                sessionManager.saveUserId(it.id)
                                sessionManager.saveUsername(it.username)
                            }
                        }
                        navController.navigate("chat-list") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } else {
                    val errorJson = response.errorBody()?.string()
                    val errorObj = Gson().fromJson(errorJson, AuthResponse::class.java)
                    errorMessage = errorObj?.message ?: "Invalid data"
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    sessionManager.clearSession()
                    navController.navigate("login")
                } else {
                    errorMessage = "Login failed"
                }
            }catch (e: Exception) {
                errorMessage = "Bad connection: " + e.message
            } finally {
                isLoading = false
            }
        }
    }






}