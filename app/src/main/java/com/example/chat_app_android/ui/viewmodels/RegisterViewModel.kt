package com.example.chat_app_android.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    var email by mutableStateOf("")
        private set

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var registrationSucceeded by mutableStateOf(false)
        private set

    val isEmailValid: Boolean
        get() = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    val isUsernameValid: Boolean
        get() = username.length >= 3

    val isPasswordValid: Boolean
        get() = password.length >= 6

    val isFormValid: Boolean
        get() = isEmailValid && isUsernameValid && isPasswordValid

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun clearSuccessMessage() {
        successMessage = null
    }

    fun consumeRegistrationSuccess() {
        registrationSucceeded = false
    }

    fun registerUser() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val request = AuthRequest(
                    email = email,
                    password = password,
                    username = username
                )

                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful) {
                    val authBody = response.body()
                    successMessage = authBody?.message ?: "Успешна регистрация"
                    registrationSucceeded = true
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