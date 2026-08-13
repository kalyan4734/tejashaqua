package com.tejashaqua.app

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.messaging.FirebaseMessaging
import com.tejashaqua.app.utils.LocaleHelper
import java.util.Locale

class TejashAquaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applySavedLocale(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // Subscribe to all_listings topic for push notifications
        FirebaseMessaging.getInstance().subscribeToTopic("all_listings")
        // Subscribe to all_users topic for rate updates
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        
        if (!Places.isInitialized()) {
            val lang = LocaleHelper.getSelectedLanguage(this) ?: "en"
            val locale = Locale.forLanguageTag(lang)
            Places.initialize(this, "AIzaSyC1H1cs7ieShx2j_s66MsOJO6YOi5S_TjQ", locale)
        }
    }
}
