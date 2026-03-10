package com.example.communitys.utils

object ValidationHelper {

    // ─────────────────────────────────────────────────────────────────────────
    // ValidationResult — nested so AuthRepository & AuthViewModel can use
    // ValidationHelper.ValidationResult.Error / .Success directly
    // ─────────────────────────────────────────────────────────────────────────
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    // ── Regex patterns ────────────────────────────────────────────────────────

    // Letters + allowed punctuation: . - '  |  min 2 chars  |  no digits/spaces
    private val NAME_REGEX = Regex("^[A-Za-zÀ-ÖØ-öø-ÿ][A-Za-zÀ-ÖØ-öø-ÿ.'\\-]*\$")

    // @gmail.com only
    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9._%+\\-]+@gmail\\.com\$",
        RegexOption.IGNORE_CASE
    )

    // Letters + digits only — no symbols, no whitespace
    private val PASSWORD_REGEX = Regex("^[A-Za-z0-9]+\$")

    // Exactly 6 digits
    private val OTP_REGEX = Regex("^[0-9]{6}\$")

    // ─────────────────────────────────────────────────────────────────────────
    // NAME   (used by AuthRepository + AuthViewModel as validateName())
    // Rules: letters + . - '  only | min 2 chars | no digits | no whitespace
    // ─────────────────────────────────────────────────────────────────────────
    fun validateName(value: String, fieldLabel: String = "Name"): ValidationResult {
        val name = value.trim()
        return when {
            name.isEmpty()            -> ValidationResult.Error("$fieldLabel is required")
            name.length < 2           -> ValidationResult.Error("$fieldLabel must be at least 2 characters")
            name.any { it.isDigit() } -> ValidationResult.Error("$fieldLabel cannot contain numbers")
            !NAME_REGEX.matches(name) -> ValidationResult.Error("$fieldLabel: only letters and . - ' allowed")
            else                      -> ValidationResult.Success
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMAIL   (used by AuthRepository, AuthViewModel, SignUpActivity)
    // Rules: valid format | @gmail.com only | no leading/trailing spaces
    // ─────────────────────────────────────────────────────────────────────────
    fun validateEmail(value: String): ValidationResult {
        val email = value.trim()
        return when {
            email.isEmpty()             -> ValidationResult.Error("Email is required")
            !email.contains("@")        -> ValidationResult.Error("Enter a valid email address")
            !EMAIL_REGEX.matches(email) -> ValidationResult.Error("Only @gmail.com addresses are accepted")
            else                        -> ValidationResult.Success
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASSWORD   (used by AuthRepository, AuthViewModel, SignUpActivity)
    // Rules: alphanumeric only | no whitespace | min 8 chars | 1 letter + 1 digit
    // ─────────────────────────────────────────────────────────────────────────
    fun validatePassword(value: String): ValidationResult {
        return when {
            value.isEmpty()                 -> ValidationResult.Error("Password is required")
            value.any { it.isWhitespace() } -> ValidationResult.Error("Password must not contain spaces")
            value.length < 8                -> ValidationResult.Error("Password must be at least 8 characters")
            !PASSWORD_REGEX.matches(value)  -> ValidationResult.Error("Only letters and numbers allowed (no symbols)")
            !value.any { it.isLetter() }    -> ValidationResult.Error("Password must contain at least one letter")
            !value.any { it.isDigit() }     -> ValidationResult.Error("Password must contain at least one number")
            else                            -> ValidationResult.Success
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BARANGAY   (used by AuthRepository + AuthViewModel)
    // ─────────────────────────────────────────────────────────────────────────
    fun validateBarangay(value: String): ValidationResult {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty()  -> ValidationResult.Error("Barangay is required")
            trimmed.length < 2 -> ValidationResult.Error("Please enter a valid barangay name")
            else               -> ValidationResult.Success
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP   (used by AuthRepository verifyEmail)
    // Rules: exactly 6 digits
    // ─────────────────────────────────────────────────────────────────────────
    fun validateOTP(value: String): ValidationResult {
        val otp = value.trim()
        return when {
            otp.isEmpty()           -> ValidationResult.Error("OTP is required")
            !OTP_REGEX.matches(otp) -> ValidationResult.Error("OTP must be exactly 6 digits")
            else                    -> ValidationResult.Success
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM PASSWORD   (used by SignUpActivity)
    // ─────────────────────────────────────────────────────────────────────────
    fun validateConfirmPassword(password: String, confirm: String): ValidationResult {
        return when {
            confirm.isEmpty()   -> ValidationResult.Error("Please confirm your password")
            confirm != password -> ValidationResult.Error("Passwords do not match")
            else                -> ValidationResult.Success
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXTENSION — lets SignUpActivity extract String? for TextInputLayout.error
    // Usage:  binding.tilEmail.error = validateEmail(s).errorMessage()
    // ─────────────────────────────────────────────────────────────────────────
    fun ValidationResult.errorMessage(): String? =
        if (this is ValidationResult.Error) message else null
}