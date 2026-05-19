package com.tejashaqua.app

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.tejashaqua.app.utils.LocaleHelper

class TejashAquaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleHelper.applySavedLocale(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Subscribe to all_listings topic for push notifications
        FirebaseMessaging.getInstance().subscribeToTopic("all_listings")
        
        if (!Places.isInitialized()) {
            // Replace with actual API key if needed
            Places.initialize(this, "AIzaSyD5VUXOhcaF840JnM5YaUMIH1cx2Qdj4QM")
        }
    }
}
