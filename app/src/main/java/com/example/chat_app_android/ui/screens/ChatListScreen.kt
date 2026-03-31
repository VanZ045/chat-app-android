package com.example.chat_app_android.ui.screens


import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.chat_app_android.data.local.SessionManager

@Composable
fun ChatListScreen(navController: NavController){
    val context = LocalContext.current

    Button(onClick = {val sessionManager = SessionManager(context)
        sessionManager.clearSession()
        navController.navigate("login")}) {
        Text(text = "Logout")
    }
}