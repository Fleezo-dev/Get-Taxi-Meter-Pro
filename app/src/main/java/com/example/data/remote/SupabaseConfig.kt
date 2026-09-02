package com.example.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    const val SUPABASE_URL = "https://hfeukzpzjuqkdigdcbam.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhmZXVrenB6anVxa2RpZ2RjYmFtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgxMDc0OTAsImV4cCI6MjEwMzY4MzQ5MH0.3lnvgQCWoVcKdGIiwFuHK8qWFqb9NETBO789YblB0A4"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }
    }
}
