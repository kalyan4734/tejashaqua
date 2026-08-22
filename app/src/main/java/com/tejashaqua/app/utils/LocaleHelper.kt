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
    }

    fun getSelectedLanguage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null)
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
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getSelectedLanguage(context) ?: return context
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        // Explicitly update configuration for the current resources (helps with older devices)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        return context.createConfigurationContext(configuration)
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
