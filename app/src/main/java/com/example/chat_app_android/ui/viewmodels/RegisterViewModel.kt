package com.example.chat_app_android.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel(){
    var isLoading by mutableStateOf(false)
    var email by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)

    fun registerUser(navController: NavController){
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val request = AuthRequest(email = email , password = password , username = username)
                val response = RetrofitClient.apiService.register(request)

                if (response.token != null){
                    //navigation to chatscreen
                }else{
                    errorMessage = response.message
                }
            }catch (e: Exception){
                errorMessage = "Error with server connection"
            }finally {
                isLoading = false
            }
        }
    }
}