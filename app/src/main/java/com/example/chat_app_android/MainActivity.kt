package com.example.chat_app_android

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.chat_app_android.ui.screens.App


class MainActivity : ComponentActivity() {

    var pendingChatId: Long = -1L
    var pendingOtherUsername: String = ""
    var pendingProfileImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val openChat = intent.getBooleanExtra("open_chat", false)
        if (openChat) {
            pendingChatId = intent.getLongExtra("chat_id", -1L)
            pendingOtherUsername = intent.getStringExtra("other_username") ?: ""
            return
        }


        val fcmChatId = intent.getStringExtra("chatId")?.toLongOrNull() ?: -1L
        val fcmUsername = intent.getStringExtra("senderUsername") ?: ""
        val fcmProfileImageUrl = intent.getStringExtra("senderProfileImageUrl") ?: ""
        if (fcmChatId != -1L) {
            pendingChatId = fcmChatId
            pendingOtherUsername = fcmUsername
            pendingProfileImageUrl = fcmProfileImageUrl
        }
    }
}

