package com.example.chat_app_android.data.models

data class ChatUpdateEvent(
    val chatId: Long,
    val lastMessage: String,
    val lastMessageTime: String?,
    val lastMessageSenderId: Long
)