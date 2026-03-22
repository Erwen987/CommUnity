package com.example.communitys.model.repository

import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.UserModel
import com.example.communitys.utils.ValidationHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
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

                val user = users.first()
                if (user.isBanned == true) {
                    try { supabase.auth.signOut() } catch (_: Exception) {}
                    val reason = if (!user.banReason.isNullOrBlank()) "\n\nReason: ${user.banReason}" else ""
                    throw Exception("Your account has been suspended.$reason\n\nPlease contact your barangay official for more information.")
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
            supabase.auth.resetPasswordForEmail(email, "communitys://reset-password")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send reset email"))
        }
    }

    suspend fun updatePasswordWithSession(accessToken: String, refreshToken: String, newPassword: String): Result<Unit> {
        return try {
            supabase.auth.importSession(
                io.github.jan.supabase.gotrue.user.UserSession(
                    accessToken  = accessToken,
                    tokenType    = "bearer",
                    expiresIn    = 3600L,
                    refreshToken = refreshToken,
                    user         = null
                )
            )
            supabase.auth.updateUser { password = newPassword }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to reset password"))
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
    // ── Update Avatar ─────────────────────────────────────────────────────────

    suspend fun updateAvatar(avatarUrl: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw Exception("Not authenticated")
            supabase.from("users")
                .update({ set("avatar_url", avatarUrl) }) {
                    filter { eq("auth_id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update avatar: ${e.message}"))
        }
    }

    // ── Upload Avatar Photo to Storage ────────────────────────────────────────

    suspend fun uploadAvatarPhoto(userId: String, imageBytes: ByteArray): Result<String> {
        return try {
            val path = "$userId/avatar.jpg"
            supabase.storage.from("avatars").upload(path, imageBytes, upsert = true)
            // Append timestamp so Coil doesn't serve the old cached image
            val url = supabase.storage.from("avatars").publicUrl(path) + "?t=${System.currentTimeMillis()}"
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload photo: ${e.message}"))
        }
    }

    // ── Send OTP for Identity Verification ───────────────────────────────────
    // Uses resetPasswordForEmail which sends a 6-digit code (not a magic link)

    suspend fun sendOtpForVerification(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to send verification code: ${e.message}"))
        }
    }

    // ── Verify OTP for Sensitive Action ──────────────────────────────────────

    suspend fun verifyOtpForAction(email: String, otp: String): Result<Unit> {
        return try {
            when (val result = ValidationHelper.validateOTP(otp)) {
                is ValidationHelper.ValidationResult.Error ->
                    return Result.failure(Exception(result.message))
                else -> {}
            }
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.RECOVERY,
                email = email,
                token = otp
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("invalid", ignoreCase = true) == true ->
                    "Invalid verification code. Please try again."
                e.message?.contains("expired", ignoreCase = true) == true ->
                    "Code has expired. Please request a new one."
                else -> "Verification failed: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    // ── Change Password ───────────────────────────────────────────────────────

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val email = supabase.auth.currentUserOrNull()?.email
                ?: throw Exception("Not authenticated")

            // Verify current password
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = currentPassword
                }
            } catch (e: Exception) {
                throw Exception("Current password is incorrect")
            }

            // Update to new password
            supabase.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to change password"))
        }
    }

    // ── Delete Account ────────────────────────────────────────────────────────

    suspend fun deleteAccount(currentPassword: String, reason: String): Result<Unit> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: throw Exception("Not authenticated")
            val email = currentUser.email ?: throw Exception("Not authenticated")

            // Verify current password (re-authenticate so session is fresh)
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = currentPassword
                }
            } catch (e: Exception) {
                throw Exception("Password is incorrect. Account not deleted.")
            }

            android.util.Log.d("AuthRepository", "Calling delete_user_account RPC")

            // Call the RPC which:
            // 1. Logs deletion to deleted_accounts
            // 2. Deletes from users table
            // 3. Deletes from auth.users (frees the email for re-registration)
            supabase.postgrest.rpc(
                "delete_user_account",
                buildJsonObject { put("p_reason", reason) }
            )

            android.util.Log.d("AuthRepository", "Account deleted via RPC")

            // Sign out the local session
            try { supabase.auth.signOut() } catch (_: Exception) {}

            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Delete error: ${e.message}", e)
            Result.failure(Exception("Failed to delete account: ${e.message}"))
        }
    }
}

