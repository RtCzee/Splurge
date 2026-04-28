package com.example.splurge.data

import android.content.Context

/**
 * Persists the signed-in user id for simple local authentication on this device.
 */
class AuthSessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveUserSession(userId: Long, keepSignedIn: Boolean = true) {
        preferences.edit()
            .putLong(KEY_USER_ID, userId)
            .putBoolean(KEY_KEEP_SIGNED_IN, keepSignedIn)
            .apply()
    }

    fun clearUserSession() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_KEEP_SIGNED_IN)
            .apply()
    }

    fun getSignedInUserId(): Long? {
        val storedId = preferences.getLong(KEY_USER_ID, NO_USER_ID)
        return storedId.takeIf { it != NO_USER_ID }
    }

    fun shouldKeepUserSignedIn(): Boolean {
        return preferences.getBoolean(KEY_KEEP_SIGNED_IN, false)
    }

    companion object {
        private const val PREFS_NAME = "splurge_auth_session"
        private const val KEY_USER_ID = "signed_in_user_id"
        private const val KEY_KEEP_SIGNED_IN = "keep_signed_in"
        private const val NO_USER_ID = -1L
    }
}
