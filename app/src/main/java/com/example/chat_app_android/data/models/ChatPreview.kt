package com.example.chat_app_android.data.models

data class ChatPreview(
    val user: UserModel,
    val lastMessage: String,
    val lastMessageTime: String,
    val isMine: Boolean
)