package com.example.bloom

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class BloomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase early but don't block
        try {
            FirebaseApp.initializeApp(this)
            Log.d("BloomApp", "Firebase initialized")
        } catch (e: Exception) {
            Log.e("BloomApp", "Firebase init failed", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // Clean up when memory is low
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                // Release database connection
                try {
                    com.example.bloom.database.PlantDatabase.closeDatabase()
                    Log.d("BloomApp", "Database closed due to memory pressure")
                } catch (e: Exception) {
                    Log.e("BloomApp", "Error closing database", e)
                }
            }
        }
    }
}