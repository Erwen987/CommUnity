package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.UserModel
import com.example.communitys.utils.ValidationHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val supabase = CommUnityApplication.supabase

    // ── Sign Up ───────────────────────────────────────────────────────────────
    // Passes firstName, lastName, barangay as metadata so the DB trigger
    // can automatically create the profile row when OTP is verified.

    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        barangay: String
    ): Result<String> {
        return try {
            // Validate
            ValidationHelper.validateName(firstName, "First name").let {
                if (it is ValidationHelper.ValidationResult.Error)
                    return Result.failure(Exception(it.message))
            }
            ValidationHelper.validateName(lastName, "Last name").let {
                if (it is ValidationHelper.ValidationResult.Error)
                    return Result.failure(Exception(it.message))
            }
            ValidationHelper.validateEmail(email).let {
                if (it is ValidationHelper.ValidationResult.Error)
                    return Result.failure(Exception(it.message))
            }
            ValidationHelper.validatePassword(password).let {
                if (it is ValidationHelper.ValidationResult.Error)
                    return Result.failure(Exception(it.message))
            }
            ValidationHelper.validateBarangay(barangay).let {
                if (it is ValidationHelper.ValidationResult.Error)
                    return Result.failure(Exception(it.message))
            }

            // ✅ Pass user details as metadata — trigger will use these
            android.util.Log.d("AuthRepository", "Signing up user: $email")
            android.util.Log.d("AuthRepository", "Metadata - firstName: $firstName, lastName: $lastName, barangay: $barangay")

            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("first_name", firstName)
                    put("last_name", lastName)
                    put("barangay", barangay)
                }
            }

            android.util.Log.d("AuthRepository", "Sign up successful, metadata saved")

            try { supabase.auth.signOut() } catch (_: Exception) {}

            Result.success("Registration successful! Please check your email for the OTP code.")

        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("rate limit", ignoreCase = true) == true ->
                    "Too many signup attempts. Please wait a few minutes."
                e.message?.contains("already registered", ignoreCase = true) == true ||
                        e.message?.contains("duplicate", ignoreCase = true) == true ||
                        e.message?.contains("User already registered", ignoreCase = true) == true ->
                    "This email is already registered. Please login."
                else -> "Registration failed: ${e.message ?: "Unknown error"}"
            }
            android.util.Log.e("AuthRepository", "Signup error: ${e.message}", e)
            Result.failure(Exception(errorMsg))
        }
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────

    suspend fun verifyEmail(
        email: String,
        otp: String,
        firstName: String = "",
        lastName: String = "",
        barangay: String = ""
    ): Result<Unit> {
        return try {
            when (val result = ValidationHelper.validateOTP(otp)) {
                is ValidationHelper.ValidationResult.Error ->
                    return Result.failure(Exception(result.message))
                else -> {}
            }

            android.util.Log.d("AuthRepository", "Verifying OTP for email: $email")
            android.util.Log.d("AuthRepository", "User details - Name: $firstName $lastName, Barangay: $barangay")

            // Verify OTP - this will trigger the database function handle_new_user()
            // which automatically creates the user profile from raw_user_meta_data
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.SIGNUP,
                email = email,
                token = otp
            )

            android.util.Log.d("AuthRepository", "OTP verified successfully. Database trigger should have created profile.")

            // Wait a moment for the trigger to complete
            kotlinx.coroutines.delay(1000)

            // Sign out — user must log in manually
            try {
                supabase.auth.signOut()
                android.util.Log.d("AuthRepository", "User signed out after verification")
            } catch (_: Exception) {}

            Result.success(Unit)

        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("invalid", ignoreCase = true) == true -> "Invalid OTP code"
                e.message?.contains("expired", ignoreCase = true) == true -> "OTP has expired. Please request a new one."
                else -> "Verification failed: ${e.message}"
            }
            android.util.Log.e("AuthRepository", "Verification error: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        }
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────

    suspend fun resendOTP(email: String): Result<Unit> {
        return try {
            supabase.auth.resendEmail(
                type = OtpType.Email.SIGNUP,
                email = email
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Login — replace your existing login() function with this ─────────────

    suspend fun login(email: String, password: String): Result<UserModel> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("Authentication failed")

            val users = supabase.from("users")
                .select { filter { eq("auth_id", userId) } }
                .decodeList<UserModel>()

            if (users.isEmpty()) {
                // Profile was deleted — sign out and block login
                try { supabase.auth.signOut() } catch (_: Exception) {}
                throw Exception("This account has been deleted. Please sign up again with a new barangay.")
            }

            Result.success(users.first())

        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Invalid login", ignoreCase = true) == true ->
                    "Invalid email or password. Please try again."
                e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                    "Please verify your email first."
                e.message?.contains("deleted", ignoreCase = true) == true ->
                    e.message ?: "This account has been deleted."
                e.message?.contains("not found", ignoreCase = true) == true ||
                        e.message?.contains("User profile not found", ignoreCase = true) == true ->
                    "Account not found. Please sign up first."
                else -> "Login failed: ${e.message ?: "Please check your credentials"}"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    // ── Sign In (alias used by AuthViewModel) ─────────────────────────────────

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Check if profile exists
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val users = supabase.from("users")
                    .select { filter { eq("auth_id", userId) } }
                    .decodeList<UserModel>()

                if (users.isEmpty()) {
                    // Profile doesn't exist - orphaned auth user
                    try {
                        supabase.auth.signOut()
                        android.util.Log.d("AuthRepository", "Signed out orphaned auth user")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthRepository", "Failed to sign out: ${e.message}")
                    }
                    throw Exception("Your account was deleted. Please sign up again.")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Invalid login", ignoreCase = true) == true ->
                    "Invalid email or password"
                e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                    "Please verify your email first"
                e.message?.contains("deleted", ignoreCase = true) == true ->
                    e.message ?: "Account was deleted"
                else -> e.message ?: "Login failed"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    suspend fun logout(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send reset email"))
        }
    }

    // ── Is Logged In ──────────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    // ── Get Current User ──────────────────────────────────────────────────────

    suspend fun getCurrentUser(): Result<UserModel> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("Not authenticated")

            android.util.Log.d("AuthRepository", "Fetching user profile for auth_id: $userId")

            val users = supabase.from("users")
                .select { filter { eq("auth_id", userId) } }
                .decodeList<UserModel>()

            android.util.Log.d("AuthRepository", "Found ${users.size} user(s)")

            if (users.isEmpty()) {
                android.util.Log.e("AuthRepository", "No user profile found for auth_id: $userId")

                // Profile doesn't exist - this is an orphaned auth user
                // Sign them out so they can register again
                try {
                    supabase.auth.signOut()
                    android.util.Log.d("AuthRepository", "Signed out orphaned auth user")
                } catch (e: Exception) {
                    android.util.Log.w("AuthRepository", "Failed to sign out: ${e.message}")
                }

                throw Exception("Your account was deleted. Please sign up again if needed.")
            }

            val user = users.first()
            android.util.Log.d("AuthRepository", "User profile loaded: ${user.firstName} ${user.lastName}")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error getting current user: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Update User Profile ───────────────────────────────────────────────────

    suspend fun updateUserProfile(
        firstName: String,
        lastName: String,
        barangay: String,
        phone: String = ""
    ): Result<UserModel> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("Not authenticated")

            // Update the user record in the database
            supabase.from("users")
                .update({
                    set("first_name", firstName)
                    set("last_name", lastName)
                    set("barangay", barangay)
                    set("phone", phone)
                }) {
                    filter { eq("auth_id", userId) }
                }

            // Fetch and return the updated user
            val users = supabase.from("users")
                .select { filter { eq("auth_id", userId) } }
                .decodeList<UserModel>()

            if (users.isEmpty()) throw Exception("User profile not found")

            Result.success(users.first())
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update profile: ${e.message}"))
        }
    }
    // ── Change Password ───────────────────────────────────────────────────────

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            supabase.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to change password: ${e.message}"))
        }
    }

    // ── Delete Account ────────────────────────────────────────────────────────
    // Calls the Supabase function that:
    // 1. Logs deletion to deleted_accounts table
    // 2. Deletes from users table
    // 3. Deletes from auth.users (making email available for reuse)

    suspend fun deleteAccount(reason: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("Not authenticated")

            android.util.Log.d("AuthRepository", "Deleting account for auth_id: $userId, reason: $reason")

            // Manual deletion process
            // Step 1: Get user profile details
            val users = try {
                supabase.from("users")
                    .select { filter { eq("auth_id", userId) } }
                    .decodeList<UserModel>()
            } catch (_: Exception) { emptyList() }

            val profile = users.firstOrNull()
            val email = supabase.auth.currentUserOrNull()?.email ?: ""

            // Step 2: Log deletion to deleted_accounts
            try {
                supabase.from("deleted_accounts")
                    .insert(mapOf(
                        "auth_id" to userId.toString(),
                        "email" to email,
                        "first_name" to (profile?.firstName ?: ""),
                        "last_name" to (profile?.lastName ?: ""),
                        "barangay" to (profile?.barangay ?: ""),
                        "reason" to reason
                    ))
                android.util.Log.d("AuthRepository", "Deletion logged to deleted_accounts")
            } catch (logError: Exception) {
                android.util.Log.w("AuthRepository", "Could not log deletion: ${logError.message}")
            }

            // Step 3: Delete from users table
            supabase.from("users")
                .delete { filter { eq("auth_id", userId) } }

            android.util.Log.d("AuthRepository", "Profile deleted from users table")

            // Step 4: Sign out locally
            try {
                supabase.auth.signOut()
                android.util.Log.d("AuthRepository", "Signed out after deletion")
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Sign out failed: ${e.message}")
            }

            android.util.Log.d("AuthRepository", "Account deletion complete. Email is now available for reuse.")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Delete error: ${e.message}", e)
            Result.failure(Exception("Failed to delete account: ${e.message}"))
        }
    }
}

