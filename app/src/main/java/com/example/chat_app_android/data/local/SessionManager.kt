package com.example.chat_app_android.data.local
import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveEmail(email: String){
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    fun fetchEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun saveUserId(userId: Long){
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun fetchUserId(): Long{
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    fun saveUsername(username: String){
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun fetchUsername(): String?{
        return prefs.getString(KEY_USERNAME, null)
    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}