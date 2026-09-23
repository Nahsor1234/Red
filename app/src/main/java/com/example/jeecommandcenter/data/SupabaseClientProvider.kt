package com.example.jeecommandcenter.data

import com.example.jeecommandcenter.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    const val AUTH_REDIRECT_URL = "sigmaje://auth/callback"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                // Native Android callback used by Supabase email confirmation links.
                scheme = "sigmaje"
                host = "auth"
                alwaysAutoRefresh = true
            }
            install(Postgrest)
        }
    }
}
