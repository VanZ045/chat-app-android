package com.example.chat_app_android.data.network

import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.models.MessageModel
import com.example.chat_app_android.data.models.UserModel
import retrofit2.Response
import retrofit2.http.*

interface ApiService{
    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest) : Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest) : Response<AuthResponse>

    @GET("api/users")
    suspend fun getAllUsers(@Header("Authorization") token: String) : Response<List<UserModel>>

    @GET("api/messages/conversation")
    suspend fun getConversation(
        @Header("Authorization") token:String,
        @Query("email1") email1: String,
        @Query("email2") email2: String
    ): Response<List<MessageModel>>

    @GET("api/messages/last")
    suspend fun getLastMessage(
        @Header("Authorization") token: String,
        @Query("email1") email1: String,
        @Query("email2") email2: String
    ): Response<MessageModel>
}