package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

    private const val PREFS_NAME = "secure_monitor_prefs"
    private const val KEY_USERNAME = "monitor_username"
    private const val KEY_PASSWORD = "monitor_password"
    private const val KEY_TARGET_URL = "monitor_target_url"

    private const val DEFAULT_USERNAME = "anbd"
    private const val DEFAULT_PASSWORD = "An@CanTho?2025"
    private const val DEFAULT_URL = "https://congan.cantho.gov.vn:8888/"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }

        return synchronized(this) {
            cachedPrefs ?: build(context.applicationContext).also { cachedPrefs = it }
        }
    }

    private fun build(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME

    fun setUsername(context: Context, username: String) {
        prefs(context).edit().putString(KEY_USERNAME, username).apply()
    }

    fun getPassword(context: Context): String =
        prefs(context).getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

    fun setPassword(context: Context, password: String) {
        prefs(context).edit().putString(KEY_PASSWORD, password).apply()
    }

    fun getTargetUrl(context: Context): String =
        prefs(context).getString(KEY_TARGET_URL, DEFAULT_URL) ?: DEFAULT_URL

    fun setTargetUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_TARGET_URL, url).apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}