package com.example.chat_app_android.data.local

import kotlinx.coroutines.flow.MutableStateFlow

object AuthEventBus{
    val sessionExpired = MutableStateFlow(false)
    fun emitSessionExpired(){sessionExpired.value = true}
}