package com.example.communitys

import io.github.jan.supabase.gotrue.auth

class SupabaseAuthHelper {

    private val supabase = CommUnityApplication.supabase

    // Check if user is currently logged in
    fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    // Returns the UUID of the currently logged-in user, or null if not logged in
    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id?.toString()
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}