package com.example.chat_app_android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chat_app_android.data.local.AuthEventBus
import com.example.chat_app_android.data.local.SessionManager
import kotlinx.coroutines.delay

@Composable
fun App() {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val sessionExpired by AuthEventBus.sessionExpired.collectAsStateWithLifecycle()

    val showLogin = sessionExpired || !sessionManager.isLoggedIn()

    LaunchedEffect(Unit) {
        val expiry = sessionManager.fetchTokenExpiry()
        val now = System.currentTimeMillis()
        val delay = expiry - now
        if (delay <= 0) {
            sessionManager.clearSession()
            AuthEventBus.emitSessionExpired()
        } else {
            delay(delay)
            sessionManager.clearSession()
            AuthEventBus.emitSessionExpired()
        }
    }

    // key() forces complete remount when showLogin changes
    // so navController and back stack are always fresh
    key(showLogin) {
        val navController = rememberNavController()
        val startDestination = if (showLogin) "login" else "chat-list"

        if (showLogin) {
            LaunchedEffect(Unit) {
                AuthEventBus.sessionExpired.value = false
            }
        }

        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("forgot-password"){ForgotPasswordScreen(navController)}
            composable(
                route = "reset-password/{email}",
                arguments = listOf(
                    navArgument("email") {type = NavType.StringType}
                )
            ){backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                ResetPasswordScreen(navController = navController, email = email)
            }
            composable("chat-list") { ChatListScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable(
                route = "chat/{chatId}/{otherUsername}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.LongType },
                    navArgument("otherUsername") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                val otherUsername = backStackEntry.arguments?.getString("otherUsername") ?: ""
                ChatScreen(navController = navController, chatId = chatId, otherUsername = otherUsername)
            }
        }
    }
}