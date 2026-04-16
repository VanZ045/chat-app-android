package com.example.chat_app_android.data.network

import com.example.chat_app_android.data.local.AuthEventBus
import com.example.chat_app_android.data.local.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // for emulator use http://10.0.2.2:8080/
    // for phone use  http://192.168.1.15:8080/
    private const val BASE_URL = "http://10.0.2.2:8080/"
    lateinit var apiService: ApiService

    fun init(sessionManager: SessionManager){
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if(response.code == 401 || response.code == 403){
                    sessionManager.clearSession()
                    AuthEventBus.emitSessionExpired()
                }
                response
            }
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}