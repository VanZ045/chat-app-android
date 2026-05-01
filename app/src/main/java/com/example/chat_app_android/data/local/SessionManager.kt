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

    fun fetchTokenExpiry(): Long {
        val token = fetchAuthToken() ?: return 0L
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
            val json = String(decoded)
            val expSeconds = org.json.JSONObject(json).getLong("exp")
            expSeconds * 1000L
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Failed to parse token expiry", e)
            0L
        }
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