package com.example.chat_app_android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.chat_app_android.MainActivity
import com.example.chat_app_android.R
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.DeviceTokenRequest
import com.example.chat_app_android.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val sessionManager = SessionManager(applicationContext)
        val authToken = sessionManager.fetchAuthToken()

        if (authToken.isNullOrBlank()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.saveDeviceToken(
                    token = "Bearer $authToken",
                    request = DeviceTokenRequest(token)
                )
            } catch (e: Exception) {
                Log.e("FCM_DEBUG", "Failed to send token from onNewToken", e)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "Ново съобщение"
        val body = message.notification?.body ?: "Получихте съобщение"
        val chatId = message.data["chatId"]?.toLongOrNull() ?: -1L
        val otherUsername = message.data["senderUsername"] ?: ""

        showNotification(title, body, chatId, otherUsername)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, body: String, chatId: Long, otherUsername: String) {
        val channelId = "chat_messages"

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat", true)
            putExtra("chat_id", chatId)
            putExtra("other_username", otherUsername)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(), notification)
    }
}