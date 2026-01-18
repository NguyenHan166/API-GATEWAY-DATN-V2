package com.asoft.artsal.photo.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SharePreference @Inject constructor(
    val sharedPreferences: SharedPreferences,
    val gson: Gson
) {

    inline fun <reified T> save(key: String, any: T) {
        sharedPreferences.edit() {
            when (any) {
                is String -> putString(key, any)
                is Float -> putFloat(key, any)
                is Int -> putInt(key, any)
                is Long -> putLong(key, any)
                is Boolean -> putBoolean(key, any)
                else -> putString(key, gson.toJson(any))
            }
        }
    }

    inline fun <reified T> get(key: String, default: Any? = null): T? {
        when (T::class) {
            Float::class -> return sharedPreferences.getFloat(key, default as Float) as? T
            Int::class -> return sharedPreferences.getInt(key, default as Int) as? T
            Long::class -> return sharedPreferences.getLong(key, default as Long) as? T
            String::class -> return sharedPreferences.getString(key, default as? String) as? T
            Boolean::class -> return sharedPreferences.getBoolean(key, default as Boolean) as? T
            else -> {
                val any = sharedPreferences.getString(key, "")
                if (!any.isNullOrEmpty()) {
                    return gson.fromJson(any, T::class.java)
                }
            }
        }
        return null
    }

    fun clearAll() {
        sharedPreferences.edit().run {
            remove(LANGUAGE_APP).apply()
        }
    }

    companion object {
        const val NAME = "PhotoEditor"
        const val MODE = Context.MODE_PRIVATE
        const val IS_FIRST_OPEN_APP = "is_first_open_app"
        const val LANGUAGE_APP = "language_app"
        const val IS_ENABLE_CONFIG_ALL = "is_enable_config_all"
        const val IS_ENABLE_NOTIFY = "is_enable_notify"
        const val LAST_RATE_TIME = "last_review_time"
        const val IS_RATE_APP = "is_rate_app"
        const val IS_ENABLE_SHOW_RATE = "is_should_show_rate"
        const val CONFIG_ALL = "config_all"
        const val CONFIG_DEFAULT = "config_default"
        const val IS_SETTING_AUTO_START = "is_setting_auto_start"
    }

}
