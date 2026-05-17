package com.stemaker.openromme.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Speichert das JWT-Token sicher verschlüsselt auf dem Gerät.
 */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "romme_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var jwt: String?
        get() = prefs.getString("jwt", null)
        set(value) = prefs.edit().putString("jwt", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
