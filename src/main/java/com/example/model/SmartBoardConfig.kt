package com.example.model

import android.content.Context
import android.content.SharedPreferences

data class SmartBoardConfig(
    val botToken: String = "",
    val botUsername: String = "SmartBoard86_bot",
    val boardId: String = "DOSKA-86-01",
    val adminPin: String = "1234",
    val defaultSessionMinutes: Int = 45,
    val warningMinutes: Int = 5,
    val wallpaperUrl: String = "https://sam-corp-live-wallpaper.vercel.app/",
    val useBuiltinWallpaper: Boolean = false,
    val keepScreenAwake: Boolean = true
)

class SmartBoardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smartboard_86_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): SmartBoardConfig {
        return SmartBoardConfig(
            botToken = prefs.getString(KEY_BOT_TOKEN, "") ?: "",
            botUsername = prefs.getString(KEY_BOT_USERNAME, "SmartBoard86_bot") ?: "SmartBoard86_bot",
            boardId = prefs.getString(KEY_BOARD_ID, "DOSKA-86-01") ?: "DOSKA-86-01",
            adminPin = prefs.getString(KEY_ADMIN_PIN, "1234") ?: "1234",
            defaultSessionMinutes = prefs.getInt(KEY_SESSION_MINUTES, 45),
            warningMinutes = prefs.getInt(KEY_WARNING_MINUTES, 5),
            wallpaperUrl = prefs.getString(KEY_WALLPAPER_URL, "https://sam-corp-live-wallpaper.vercel.app/") ?: "https://sam-corp-live-wallpaper.vercel.app/",
            useBuiltinWallpaper = prefs.getBoolean(KEY_USE_BUILTIN_WALLPAPER, false),
            keepScreenAwake = prefs.getBoolean(KEY_KEEP_AWAKE, true)
        )
    }

    fun saveConfig(config: SmartBoardConfig) {
        prefs.edit().apply {
            putString(KEY_BOT_TOKEN, config.botToken)
            putString(KEY_BOT_USERNAME, config.botUsername)
            putString(KEY_BOARD_ID, config.boardId)
            putString(KEY_ADMIN_PIN, config.adminPin)
            putInt(KEY_SESSION_MINUTES, config.defaultSessionMinutes)
            putInt(KEY_WARNING_MINUTES, config.warningMinutes)
            putString(KEY_WALLPAPER_URL, config.wallpaperUrl)
            putBoolean(KEY_USE_BUILTIN_WALLPAPER, config.useBuiltinWallpaper)
            putBoolean(KEY_KEEP_AWAKE, config.keepScreenAwake)
            apply()
        }
    }

    companion object {
        private const val KEY_BOT_TOKEN = "key_bot_token"
        private const val KEY_BOT_USERNAME = "key_bot_username"
        private const val KEY_BOARD_ID = "key_board_id"
        private const val KEY_ADMIN_PIN = "key_admin_pin"
        private const val KEY_SESSION_MINUTES = "key_session_minutes"
        private const val KEY_WARNING_MINUTES = "key_warning_minutes"
        private const val KEY_WALLPAPER_URL = "key_wallpaper_url"
        private const val KEY_USE_BUILTIN_WALLPAPER = "key_use_builtin_wallpaper"
        private const val KEY_KEEP_AWAKE = "key_keep_awake"
    }
}
