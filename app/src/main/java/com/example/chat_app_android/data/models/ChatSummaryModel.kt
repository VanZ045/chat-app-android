package com.example.chat_app_android.data.models

data class ChatSummaryModel(
    val chatId: Long,
    val otherUserId: Long,
    val otherUsername: String,
    val otherUserProfileImageUrl: String?,
    val lastMessage: String,
    val lastMessageTime: String?,
    val lastMessageSenderId: Long?
)