package com.example.chat_app_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chat_app_android.R
import com.example.chat_app_android.ui.viewmodels.RegisterViewModel

@Composable
fun RegisterScreen(navController: NavController , viewModel: RegisterViewModel = viewModel()){

    var username by remember {
        mutableStateOf("")
    }
    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    var usernameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEDE8E6)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Image(painter = painterResource(id = R.drawable.edit),
            contentDescription = "Login image", modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Welcome to the register screen!", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(value = username, onValueChange = {username=it }, label = {
            Text(text = "Username")}, modifier = Modifier,leadingIcon = {
            Icon(Icons.Default.Face, contentDescription = null)
        },shape = RoundedCornerShape(16.dp), isError = usernameError)

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(value = email, onValueChange = {email=it }, label = {
            Text(text = "Email address")}, modifier = Modifier,leadingIcon = {
            Icon(Icons.Default.Email, contentDescription = null)
        },shape = RoundedCornerShape(16.dp), isError = emailError)

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(value = password, onValueChange = {password=it }, label = {
            Text(text = "Password")} , visualTransformation = PasswordVisualTransformation(),leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        }, shape = RoundedCornerShape(16.dp), isError = passwordError)

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(value = confirmPassword, onValueChange = {confirmPassword=it }, label = {
            Text(text = "Confirm password")} , visualTransformation = PasswordVisualTransformation(),leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        }, shape = RoundedCornerShape(16.dp), isError = confirmPasswordError)

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,){
            Button(
                onClick = {
                    emailError = email.isBlank()
                    passwordError = password.isBlank()
                    usernameError = username.isBlank()
                    confirmPasswordError = confirmPassword.isBlank()


                    if (emailError || passwordError || usernameError || confirmPasswordError){
                        Toast.makeText(context, "Please enter password and email", Toast.LENGTH_LONG).show()
                    }else {
                        viewModel.registerUser(navController)
                    }
                },
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading){
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                }else{
                    Text(text = "Register")
                }
            }

            viewModel.errorMessage?.let {
                Text(text = it, color = Color.Red)
            }

            Button(onClick = {navController.navigate("login")}) {
                Text(text = "Go back")
            }
        }
    }
}
