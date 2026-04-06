package com.example.chat_app_android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chat_app_android.data.local.SessionManager

@Composable
fun App() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    val startDestination = if (sessionManager.isLoggedIn()) "chat-list" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
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
        composable("profile"){
            ProfileScreen(navController)
        }
        composable(route = "chat/{receiverEmail}/{receiverUsername}",
            arguments = listOf(
                navArgument("receiverEmail") {type = NavType.StringType},
                navArgument("receiverUsername") {type = NavType.StringType}
            )
        ){backStackEntry ->
            val receiverEmail = backStackEntry.arguments?.getString("receiverEmail") ?: ""
            val receiverUsername = backStackEntry.arguments?.getString("receiverUsername") ?: ""
            ChatScreen(
                navController = navController,
                receiverEmail = receiverEmail,
                receiverUsername = receiverUsername
            )
        }
    }
}