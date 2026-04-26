package com.example.chat_app_android.ui.screens

import android.app.Activity
import android.net.Uri
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
    val activity = context as? Activity
    val sessionManager = SessionManager(context)
    val sessionExpired by AuthEventBus.sessionExpired.collectAsStateWithLifecycle()

    val showLogin = sessionExpired || !sessionManager.isLoggedIn()

    LaunchedEffect(Unit) {
        val expiry = sessionManager.fetchTokenExpiry()
        val now = System.currentTimeMillis()
        val delayMs = expiry - now
        if (delayMs <= 0) {
            sessionManager.clearSession()
            AuthEventBus.emitSessionExpired()
        } else {
            delay(delayMs)
            sessionManager.clearSession()
            AuthEventBus.emitSessionExpired()
        }
    }

    key(showLogin) {
        val navController = rememberNavController()
        val startDestination = if (showLogin) "login" else "chat-list"

        if (showLogin) {
            LaunchedEffect(Unit) {
                AuthEventBus.sessionExpired.value = false
            }
        }

        LaunchedEffect(navController, showLogin) {
            if (showLogin) return@LaunchedEffect

            val intent = activity?.intent ?: return@LaunchedEffect
            val openChat = intent.getBooleanExtra("open_chat", false)
            val chatId = intent.getLongExtra("chat_id", -1L)
            val otherUsername = intent.getStringExtra("other_username").orEmpty()

            if (openChat && chatId != -1L) {
                navController.navigate("chat/$chatId/${otherUsername.encodeNavArg()}") {
                    launchSingleTop = true
                }

                intent.removeExtra("open_chat")
                intent.removeExtra("chat_id")
                intent.removeExtra("other_username")
            }
        }

        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("forgot-password") { ForgotPasswordScreen(navController) }
            composable(
                route = "reset-password/{email}",
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType }
                )
            ) { backStackEntry ->
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

private fun String.encodeNavArg(): String = Uri.encode(this)