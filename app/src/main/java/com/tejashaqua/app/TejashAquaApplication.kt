package com.tejashaqua.app

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp

class TejashAquaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        if (!Places.isInitialized()) {
            // Replace with actual API key if needed
            Places.initialize(this, "AIzaSyD5VUXOhcaF840JnM5YaUMIH1cx2Qdj4QM")
        }
    }
}
