package com.tejashaqua.app.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_LOCATION_DISCLOSURE_SHOWN = "location_disclosure_shown"

    fun setLocale(context: Context, languageCode: String) {
        // 1. Save to SharedPreferences immediately (Source of truth)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { putString(KEY_LANGUAGE, languageCode) }
        
        // 2. Apply via AppCompatDelegate
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        // 3. Update JVM default locale
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
    }

    fun getSelectedLanguage(context: Context): String? {
        // 1. Always prioritize SharedPreferences to avoid system sync "vice-versa" bugs
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return saved

        // 2. Fallback to AppCompatDelegate
        try {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            if (!currentLocales.isEmpty) {
                return currentLocales.get(0)?.toLanguageTag()
            }
        } catch (_: Exception) {}
        
        return null
    }

    fun applySavedLocale(context: Context) {
        val languageCode = getSelectedLanguage(context) ?: return
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocale.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
        Locale.setDefault(Locale.forLanguageTag(languageCode))
    }

    fun wrapContext(context: Context, languageCode: String? = null): Context {
        val code = languageCode ?: getSelectedLanguage(context) ?: return context
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        
        // Android 13+ handles this perfectly via the OS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }

        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val isOppoRealme = manufacturer.contains("oppo") || manufacturer.contains("realme")
        
        // On Oppo/Realme Android 11/12, manual wrapping in attachBaseContext 
        // conflicts with AppCompatDelegate and causes language swapping/mirroring.
        if (isOppoRealme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && languageCode == null) {
            return context
        }

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        
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
