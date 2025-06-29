package com.bookmarklocker

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

/**
 * ThemeManager: Manages theme switching functionality for the app.
 * Supports Light Mode, Dark Mode, and System Default themes.
 */
object ThemeManager {
    
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    
    // Theme mode constants
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    
    /**
     * Gets the current theme mode from SharedPreferences.
     * @param context The application context.
     * @return The current theme mode (THEME_SYSTEM, THEME_LIGHT, or THEME_DARK).
     */
    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
    }
    
    /**
     * Sets the theme mode and saves it to SharedPreferences.
     * @param context The application context.
     * @param themeMode The theme mode to set (THEME_SYSTEM, THEME_LIGHT, or THEME_DARK).
     */
    fun setThemeMode(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply()
        
        // Apply the theme immediately
        applyTheme(themeMode)
    }
    
    /**
     * Applies the theme mode to the app.
     * @param themeMode The theme mode to apply.
     */
    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    /**
     * Applies the saved theme mode to the activity.
     * Call this in onCreate() before setContentView().
     * @param activity The activity to apply the theme to.
     */
    fun applyThemeToActivity(activity: Activity) {
        val themeMode = getThemeMode(activity)
        applyTheme(themeMode)
    }
    
    /**
     * Gets the display name for a theme mode.
     * @param context The application context.
     * @param themeMode The theme mode.
     * @return The display name for the theme mode.
     */
    fun getThemeDisplayName(context: Context, themeMode: Int): String {
        return when (themeMode) {
            THEME_LIGHT -> context.getString(R.string.light_mode)
            THEME_DARK -> context.getString(R.string.dark_mode)
            THEME_SYSTEM -> context.getString(R.string.system_default)
            else -> context.getString(R.string.system_default)
        }
    }
} 