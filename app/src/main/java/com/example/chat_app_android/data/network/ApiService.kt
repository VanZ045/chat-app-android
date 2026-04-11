package com.example.chat_app_android.data.network

import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.models.ChatSummaryModel
import com.example.chat_app_android.data.models.MessageModel
import com.example.chat_app_android.data.models.SendMessageRequest
import com.example.chat_app_android.data.models.UserModel
import retrofit2.Response
import retrofit2.http.*

interface ApiService{
    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest) : Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest) : Response<AuthResponse>

    @GET("api/users/all")
    suspend fun getAllUsers(@Header("Authorization") token: String) : Response<List<UserModel>>

    @GET("api/users/me")
    suspend fun getMe(@Header("Authorization") token: String) : Response<UserModel>

    @GET("api/chats")
    suspend fun getChats(@Header("Authorization") token: String) : Response<List<ChatSummaryModel>>

    @POST("api/chats/{otherUserId}")
    suspend fun createOrGetChat(
        @Header("Authorization") token: String,
        @Path("otherUserId") otherUserId: Long
    ): Response<ChatSummaryModel>

    @GET("api/chats/{chatId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long
    ): Response<List<MessageModel>>

    @POST("api/chats/{chatId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long,
        @Body request: SendMessageRequest
    ): Response<MessageModel>

    @POST("api/chats/{chatId}/seen")
    suspend fun markAsSeen(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long
    ): Response<Unit>
}