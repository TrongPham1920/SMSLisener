package com.aquq.smslisener.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PreferenceManager {
    private const val PREF_NAME = "sms_listener_prefs"
    private const val KEY_API_DOMAIN = "api_domain"
    private const val KEY_BODY_FORMAT = "body_format"

    // Default values
    private const val DEFAULT_API_DOMAIN = "https://webhook.site/your-unique-id"
    private const val DEFAULT_BODY_FORMAT = """{
    "sender": "{sender}",
    "message": "{message}",
    "receiver": "{receiver}"
}"""

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveApiDomain(context: Context, domain: String) {
        getPreferences(context).edit { putString(KEY_API_DOMAIN, domain) }
    }

    fun getApiDomain(context: Context): String {
        return getPreferences(context).getString(KEY_API_DOMAIN, DEFAULT_API_DOMAIN) ?: DEFAULT_API_DOMAIN
    }

    fun saveBodyFormat(context: Context, format: String) {
        getPreferences(context).edit { putString(KEY_BODY_FORMAT, format) }
    }

    fun getBodyFormat(context: Context): String {
        return getPreferences(context).getString(KEY_BODY_FORMAT, DEFAULT_BODY_FORMAT) ?: DEFAULT_BODY_FORMAT
    }
}

