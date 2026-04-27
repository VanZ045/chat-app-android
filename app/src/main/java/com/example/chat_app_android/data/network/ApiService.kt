package com.example.chat_app_android.data.network

import com.example.chat_app_android.data.models.AuthRequest
import com.example.chat_app_android.data.models.AuthResponse
import com.example.chat_app_android.data.models.ChangePasswordRequest
import com.example.chat_app_android.data.models.ChangeUsernameRequest
import com.example.chat_app_android.data.models.ChatSummaryModel
import com.example.chat_app_android.data.models.DeviceTokenRequest
import com.example.chat_app_android.data.models.EditMessageRequest
import com.example.chat_app_android.data.models.ForgotPasswordRequest
import com.example.chat_app_android.data.models.MessageModel
import com.example.chat_app_android.data.models.ResetPasswordRequest
import com.example.chat_app_android.data.models.SendMessageRequest
import com.example.chat_app_android.data.models.UserModel
import okhttp3.MultipartBody
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

    @Multipart
    @POST("api/chats/{chatId}/images")
    suspend fun uploadImage(
        @Header("Authorization") token : String,
        @Path("chatId") chatId: Long,
        @Part file: MultipartBody.Part
    ) : Response<MessageModel>

    @PUT("api/chats/{chatId}/messages/{messageId}")
    suspend fun editMessage(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body request: EditMessageRequest
    ): Response<MessageModel>

    @DELETE("api/chats/{chatId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
    ): Response<Unit>

    @DELETE("api/chats/{chatId}")
    suspend fun deleteChat(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long,
    ): Response<Unit>

    @DELETE("api/users/me")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
    ): Response<Unit>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Void>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Void>

    @PUT("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token:String,
        @Body request: ChangePasswordRequest
    ): Response<Void>

    @POST("api/users/device-token")
    suspend fun saveDeviceToken(
        @Header("Authorization") token: String,
        @Body request: DeviceTokenRequest
    ): Response<Unit>

    @POST("api/chats/{chatId}/active")
    suspend fun enterChat(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long
    ): Response<Unit>

    @DELETE("api/chats/active")
    suspend fun leaveActiveChat(
        @Header("Authorization") token: String
    ): Response<Unit>

    @Multipart
    @POST("api/users/profile-image")
    suspend fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @PUT("api/users/me/username")
    suspend fun changeUsername(
        @Header("Authorization") token: String,
        @Body request: ChangeUsernameRequest
    ): Response<Void>
}