package com.example.bloom

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    const val SUPABASE_URL = "https://xygckyyshsnottpmciil.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh5Z2NreXlzaHNub3R0cG1jaWlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ0ODA1NTUsImV4cCI6MjA4MDA1NjU1NX0.IVKl69PiM3ZjZdCaiutdCFhlssAMrI1kZRIPkQnJh1Q"

    // CRITICAL: This lazy initialization MUST be accessed from the Main thread
    // because Supabase registers lifecycle observers
    val client: SupabaseClient by lazy {
        try {
            Log.d("SupabaseClient", "Initializing Supabase client")

            createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(GoTrue)
                install(Storage)
            }.also {
                Log.d("SupabaseClient", "Supabase client initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Failed to initialize Supabase client", e)
            throw e
        }
    }

    /**
     * Check if client is initialized and ready
     */
    fun isInitialized(): Boolean {
        return try {
            client
            true
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Client not initialized", e)
            false
        }
    }
}