package com.tejashaqua.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_LOCATION_DISCLOSURE_SHOWN = "location_disclosure_shown"

    fun setLocale(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { putString(KEY_LANGUAGE, languageCode) }
        
        // Apply for Android 13+ (Per-app language)
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        // Manually update for older versions and to ensure immediate resource access
        updateContextLocale(context, languageCode)
        updateContextLocale(context.applicationContext, languageCode)
        
        // Also update the default Locale for non-context-based operations
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
    }

    fun getSelectedLanguage(context: Context): String? {
        // 1. Check SharedPreferences first as it's the most reliable source for immediate read-back
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return saved

        // 2. Fallback to AppCompatDelegate (Android 13+ Per-app language)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            return currentLocales.get(0)?.toLanguageTag()
        }
        
        return null
    }

    fun applySavedLocale(context: Context) {
        val languageCode = getSelectedLanguage(context) ?: return
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        
        // Only set if different to avoid potential loops
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocale.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
        
        // Manually update configuration for the provided context (Application or Activity)
        updateContextLocale(context, languageCode)
    }

    fun updateContextLocale(context: Context, languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        // Update resources for current context
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        // Also update Application context to ensure strings are available globally
        val appContext = context.applicationContext
        if (context != appContext) {
            val appResources = appContext.resources
            val appConfig = appResources.configuration
            appConfig.setLocale(locale)
            appConfig.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            appResources.updateConfiguration(appConfig, appResources.displayMetrics)
        }
    }

    fun wrapContext(context: Context, languageCode: String? = null): Context {
        val code = languageCode ?: getSelectedLanguage(context) ?: return context
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        val newConfig = android.content.res.Configuration(configuration)
        newConfig.setLocale(locale)
        newConfig.setLayoutDirection(locale)
        
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
