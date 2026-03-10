package com.example.communitys

import android.app.Application
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

class CommUnityApplication : Application() {

    companion object {
        lateinit var supabase: io.github.jan.supabase.SupabaseClient
            private set

        lateinit var instance: CommUnityApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        // Initialize Supabase with custom JSON configuration
        supabase = createSupabaseClient(
            supabaseUrl = "https://apesvvqntqldihnzmitn.supabase.co",
            supabaseKey = "sb_publishable_mRVhQVRmCmX7mYUpy6WDxw_Nu6iaN7_"
        ) {
            install(Auth)
            install(Postgrest) {
                serializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true  // Ignore fields not in our model
                    isLenient = true
                    coerceInputValues = true
                })
            }
            install(Storage)
        }
    }
}