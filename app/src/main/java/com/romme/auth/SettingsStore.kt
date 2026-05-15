package com.romme.auth

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("romme_settings", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) { prefs.edit().putString("server_url", value).apply() }

    var socketPath: String
        get() = prefs.getString("socket_path", "/socket.io") ?: "/socket.io"
        set(value) { prefs.edit().putString("socket_path", value).apply() }

    var nextcloudUrl: String
        get() = prefs.getString("nextcloud_url", "") ?: ""
        set(value) { prefs.edit().putString("nextcloud_url", value).apply() }

    var clientId: String
        get() = prefs.getString("client_id", "") ?: ""
        set(value) { prefs.edit().putString("client_id", value).apply() }

    var clientSecret: String
        get() = prefs.getString("client_secret", "") ?: ""
        set(value) { prefs.edit().putString("client_secret", value).apply() }
}
