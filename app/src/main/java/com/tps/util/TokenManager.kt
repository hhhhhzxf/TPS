package com.tps.util

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("tps_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit { putString(KEY_TOKEN, token) }
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun clearToken() = prefs.edit { remove(KEY_TOKEN) }

    fun saveUserId(id: Long) = prefs.edit { putLong(KEY_USER_ID, id) }
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun saveRole(role: String) = prefs.edit { putString(KEY_ROLE, role) }
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)
    fun isAdmin(): Boolean = getRole() == "ADMIN"

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() = prefs.edit { clear() }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
    }
}
