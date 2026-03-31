package com.example.chat_app_android.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel(){
    var email by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)


    val isEmailValid: Boolean
        get() = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    val isUsernameValid: Boolean
        get() = username.length >= 3

    val isPasswordValid: Boolean
        get() = password.length >= 6

    val isFormValid: Boolean
        get() = isEmailValid && isUsernameValid && isPasswordValid

    fun registerUser(navController: NavController){

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val request = AuthRequest(email = email , password = password , username = username)
                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful){
                    val authBody = response.body()
                    if (authBody?.token != null){
                        successMessage = authBody.message
                        navController.navigate("login"){
                            popUpTo("register") { inclusive = true }
                        }
                    }
                }else{
                    val errorJson = response.errorBody()?.string()
                    val errorObj = Gson().fromJson(errorJson, AuthResponse::class.java)
                    errorMessage = errorObj?.message ?: "Invalid data"
                }
            }catch (e: Exception){
                errorMessage = "Bad connection: " + e.message
            }finally {
                isLoading = false
            }
        }
    }
}