package com.example.chat_app_android

import android.app.Application
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.network.RetrofitClient

class ChatApplication : Application() {
    override fun onCreate(){
        super.onCreate()
        RetrofitClient.init(SessionManager(this))

    }
}