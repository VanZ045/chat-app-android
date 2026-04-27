package com.example.chat_app_android.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

        val mainActivity = activity as? com.example.chat_app_android.MainActivity
        val pendingProfileImageUrl = mainActivity?.pendingProfileImageUrl ?: ""
        val pendingChatId = mainActivity?.pendingChatId ?: -1L
        val pendingOtherUsername = mainActivity?.pendingOtherUsername ?: ""

        if (pendingChatId != -1L) {
            mainActivity?.pendingChatId = -1L
            mainActivity?.pendingOtherUsername = ""
        }

        if (showLogin) {
            LaunchedEffect(Unit) {
                AuthEventBus.sessionExpired.value = false
            }
        }

        LaunchedEffect(Unit) {
            if (!showLogin && pendingChatId != -1L) {
                navController.navigate("chat/$pendingChatId/${pendingOtherUsername.encodeNavArg()}/${Uri.encode(pendingProfileImageUrl.ifBlank { "null" })}")
            }
        }

        NavHost(navController = navController, startDestination = if (showLogin) "login" else "chat-list") {
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("forgot-password") { ForgotPasswordScreen(navController) }
            composable(
                route = "reset-password/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                ResetPasswordScreen(navController = navController, email = email)
            }
            composable("chat-list") { ChatListScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable(
                route = "chat/{chatId}/{otherUsername}/{otherUserProfileImageUrl}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.LongType },
                    navArgument("otherUsername") { type = NavType.StringType },
                    navArgument("otherUserProfileImageUrl") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                val otherUsername = backStackEntry.arguments?.getString("otherUsername") ?: ""
                val otherUserProfileImageUrl = backStackEntry.arguments?.getString("otherUserProfileImageUrl")
                ChatScreen(
                    navController = navController,
                    chatId = chatId,
                    otherUsername = otherUsername,
                    otherUserProfileImageUrl = otherUserProfileImageUrl
                )
            }
        }
    }
}

private fun String.encodeNavArg(): String = Uri.encode(this)