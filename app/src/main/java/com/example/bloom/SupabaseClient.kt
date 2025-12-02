package com.example.bloom

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    const val SUPABASE_URL = "https://xygckyyshsnottpmciil.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh5Z2NreXlzaHNub3R0cG1jaWlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ0ODA1NTUsImV4cCI6MjA4MDA1NjU1NX0.IVKl69PiM3ZjZdCaiutdCFhlssAMrI1kZRIPkQnJh1Q"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(GoTrue)
            install(Storage)
        }
    }
}