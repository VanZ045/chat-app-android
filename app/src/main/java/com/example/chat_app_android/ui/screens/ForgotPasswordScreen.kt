package com.example.chat_app_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chat_app_android.data.models.ForgotPasswordRequest
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember {mutableStateOf(false)}
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot password") },
                navigationIcon = {
                    IconButton(onClick = {navController.popBackStack()}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Enter your email address and we'll send you a reset code",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {email = it},
                label = {Text("Email address")},
                leadingIcon = {Icon(Icons.Default.Email, contentDescription = null)},
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if(email.isBlank()){
                        Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try{
                            val response = RetrofitClient.apiService.forgotPassword(
                                ForgotPasswordRequest(email)
                            )
                            if(response.isSuccessful){
                                Toast.makeText(context, "Reset code sent to your email", Toast.LENGTH_LONG).show()
                                navController.navigate("reset-password/${email}")
                            }else{
                                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                            }
                        }catch (e: Exception){
                            Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                        }finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if(isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else Text("Send Reset Code")
            }
        }
    }
}