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
        composable(
            route = "chat/{chatId}/{otherUsername}",
            arguments = listOf(
                navArgument("chatId") {type = NavType.LongType},
                navArgument("otherUsername") {type = NavType.StringType}
            )
        ){backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val otherUsername = backStackEntry.arguments?.getString("otherUsername") ?: ""
            ChatScreen(
                navController = navController,
                chatId = chatId,
                otherUsername = otherUsername
            )
        }
    }
}