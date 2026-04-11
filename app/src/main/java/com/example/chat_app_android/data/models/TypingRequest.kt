package com.example.chat_app_android.data.models

data class TypingRequest(
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String,
    @com.google.gson.annotations.SerializedName("typing")
    val isTyping: Boolean
)