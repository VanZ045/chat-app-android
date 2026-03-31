package com.example.chat_app_android.data.local
import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }

    // ✅ Save token
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    // ✅ Get token
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // ✅ Check if logged in
    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    // ✅ Logout (clear session)
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}