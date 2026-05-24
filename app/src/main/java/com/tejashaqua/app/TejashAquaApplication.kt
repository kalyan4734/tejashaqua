package com.tejashaqua.app

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.tejashaqua.app.utils.LocaleHelper
import java.util.Locale

class TejashAquaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applySavedLocale(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Subscribe to all_listings topic for push notifications
        FirebaseMessaging.getInstance().subscribeToTopic("all_listings")
        
        if (!Places.isInitialized()) {
            val lang = LocaleHelper.getSelectedLanguage(this) ?: "en"
            val locale = Locale.forLanguageTag(lang)
            Places.initialize(this, "AIzaSyD5VUXOhcaF840JnM5YaUMIH1cx2Qdj4QM", locale)
        }
    }
}
