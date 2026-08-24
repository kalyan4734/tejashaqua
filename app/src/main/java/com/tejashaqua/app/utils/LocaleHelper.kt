package com.tejashaqua.app.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_LOCATION_DISCLOSURE_SHOWN = "location_disclosure_shown"

    fun setLocale(context: Context, languageCode: String) {
        // 1. Save to SharedPreferences for reliable read-back across all versions
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { putString(KEY_LANGUAGE, languageCode) }
        
        // 2. Apply via AppCompatDelegate (Android 13+ Per-app language and pre-13 persistence)
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        // 3. Update JVM default locale
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
    }

    fun getSelectedLanguage(context: Context): String? {
        // 1. Try AppCompatDelegate (handles both Android 13+ and pre-13 via persistence)
        try {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            if (!currentLocales.isEmpty) {
                val tag = currentLocales.get(0)?.toLanguageTag()
                if (tag != null) return tag
            }
        } catch (_: Exception) {
            // Fallback to prefs
        }

        // 2. Fallback to SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null)
    }

    fun applySavedLocale(context: Context) {
        val languageCode = getSelectedLanguage(context) ?: return
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        
        // Sync AppCompatDelegate if necessary
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocale.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
        
        // Update JVM default
        Locale.setDefault(Locale.forLanguageTag(languageCode))
    }

    /**
     * Used in attachBaseContext to wrap context with correct locale.
     * This is crucial for pre-Android 13 devices to load correct resources.
     */
    fun wrapContext(context: Context, languageCode: String? = null): Context {
        val code = languageCode ?: getSelectedLanguage(context) ?: return context
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        val newConfig = Configuration(configuration)
        
        // setLocale automatically handles layout direction (LTR/RTL) on API 17+
        newConfig.setLocale(locale)
        
        return context.createConfigurationContext(newConfig)
    }

    fun isLocationDisclosureShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOCATION_DISCLOSURE_SHOWN, false)
    }

    fun setLocationDisclosureShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_LOCATION_DISCLOSURE_SHOWN, true) }
    }
}
