package com.example.chat_app_android.data.network

import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService{
    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest) : AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest) : AuthResponse

}