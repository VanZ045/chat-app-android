package com.example.chat_app_android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chat_app_android.data.local.SessionManager

@Composable
fun App() {
    val navController = rememberNavController()
//    val context = LocalContext.current
//    val sessionManager = SessionManager(context)

//    val startDestination = if (sessionManager.isLoggedIn()) {
//        "chat-list"
//    } else {
//        "login"
//    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController)
        }
        composable("register") {
            RegisterScreen(navController)
        }
        composable("chat-list") {
            ChatListScreen(navController)
        }
    }
}