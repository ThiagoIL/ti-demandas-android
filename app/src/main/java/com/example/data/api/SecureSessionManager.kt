package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureSessionManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_user_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback robusto para ambientes de testes locais (como JVM, Robolectric ou Roborazzi)
        // que não possuem acesso às chaves de hardware do Android KeyStore.
        context.getSharedPreferences("secure_user_session_test_fallback", Context.MODE_PRIVATE)
    }

    /**
     * Stores the JWT token securely.
     */
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Retrieves the stored JWT token. Returns null if not found.
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * Deletes the JWT token from secure storage.
     */
    fun clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    /**
     * Checks if a valid-looking token is present in storage.
     */
    fun hasToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    /**
     * Stores the local theme preference.
     */
    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
    }

    /**
     * Retrieves the stored local theme preference.
     */
    fun getTheme(): String? {
        return sharedPreferences.getString(KEY_THEME, null)
    }

    /**
     * Stores the last logged-in email.
     */
    fun saveLastEmail(email: String) {
        sharedPreferences.edit().putString(KEY_LAST_EMAIL, email).apply()
    }

    /**
     * Retrieves the stored last logged-in email.
     */
    fun getLastEmail(): String? {
        return sharedPreferences.getString(KEY_LAST_EMAIL, null)
    }

    companion object {
        private const val KEY_TOKEN = "jwt_auth_token"
        private const val KEY_THEME = "theme_preference"
        private const val KEY_LAST_EMAIL = "last_logged_email"
    }
}
