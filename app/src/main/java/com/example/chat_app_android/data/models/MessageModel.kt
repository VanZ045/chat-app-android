package com.example.chat_app_android.data.models

data class MessageModel (
    val id: Long,
    val senderEmail: String,
    val receiverEmail: String,
    val content: String,
    val timestamp: String
)