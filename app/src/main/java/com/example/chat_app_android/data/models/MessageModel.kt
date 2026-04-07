package com.example.chat_app_android.data.models

data class MessageModel (
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String,
    val content: String,
    val createdAt: String
)