package com.example.chat_app_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chat_app_android.R
import com.example.chat_app_android.ui.viewmodels.RegisterViewModel

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    var usernameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(viewModel.registrationSucceeded) {
        if (viewModel.registrationSucceeded) {
            navController.navigate("login") {
                popUpTo("register") { inclusive = true }
            }
            viewModel.consumeRegistrationSuccess()
        }
    }

    viewModel.successMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.edit),
            contentDescription = "Изображение за регистрация",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Създай акаунт!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = viewModel.username,
            onValueChange = {
                viewModel.onUsernameChange(it)
                if (usernameError && it.isNotBlank()) usernameError = false
            },
            label = { Text("Потребителско име") },
            leadingIcon = {
                Icon(Icons.Default.Face, contentDescription = null)
            },
            shape = RoundedCornerShape(16.dp),
            isError = usernameError,
            placeholder = { Text("мин. 3 символа") }
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = {
                viewModel.onEmailChange(it)
                if (emailError && it.isNotBlank()) emailError = false
            },
            label = { Text("Имейл адрес") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            shape = RoundedCornerShape(16.dp),
            isError = emailError,
            placeholder = { Text("example@mail.com") }
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = {
                viewModel.onPasswordChange(it)
                if (passwordError && it.isNotBlank()) passwordError = false
            },
            label = { Text("Парола") },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Default.Visibility
                } else {
                    Icons.Default.VisibilityOff
                }

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(image, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(16.dp),
            isError = passwordError,
            placeholder = { Text("мин. 6 символа") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    emailError = viewModel.email.isBlank()
                    passwordError = viewModel.password.isBlank()
                    usernameError = viewModel.username.isBlank()

                    if (emailError || passwordError || usernameError) {
                        Toast.makeText(
                            context,
                            "Моля, въведете имейл, потребителско име и парола",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        viewModel.registerUser()
                    }
                },
                enabled = !viewModel.isLoading && viewModel.isFormValid
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Регистрация")
                }
            }

            Button(onClick = { navController.navigate("login") }) {
                Text("Назад")
            }
        }

        viewModel.errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = Color.Red)
        }
    }
}