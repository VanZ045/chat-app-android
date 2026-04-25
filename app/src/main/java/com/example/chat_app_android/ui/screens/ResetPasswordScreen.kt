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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chat_app_android.data.models.ResetPasswordRequest
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(navController: NavController, email: String) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                text = "Enter the code sent to $email and your new password.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Reset code") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm new password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (code.isBlank() || newPassword.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.resetPassword(
                                ResetPasswordRequest(code, newPassword)
                            )
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Password reset successfully!", Toast.LENGTH_LONG).show()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Invalid or expired code", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                else Text("Reset Password")
            }
        }
    }
}