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

    // Letters + spaces + allowed punctuation: . - '  |  min 2 chars  |  no digits
    private val NAME_REGEX = Regex("^[A-Za-zÀ-ÖØ-öø-ÿ][A-Za-zÀ-ÖØ-öø-ÿ .'\\-]*\$")

    // @gmail.com only
    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9._%+\\-]+@gmail\\.com\$",
        RegexOption.IGNORE_CASE
    )

    // Letters + digits + allowed special chars
    private val PASSWORD_REGEX = Regex("^[A-Za-z0-9@#\$!%*?&_\\-]+\$")

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
            !NAME_REGEX.matches(name) -> ValidationResult.Error("$fieldLabel: only letters, spaces, and . - ' allowed")
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
    // Rules: min 8 chars | 1 uppercase | 1 lowercase | 1 digit | 1 special char
    // ─────────────────────────────────────────────────────────────────────────
    private val SPECIAL_CHARS = setOf('@', '#', '$', '!', '%', '*', '?', '&', '_', '-')

    fun validatePassword(value: String): ValidationResult {
        if (value.isEmpty()) return ValidationResult.Error("Password is required")
        if (value.any { it.isWhitespace() }) return ValidationResult.Error("Password must not contain spaces")
        if (!PASSWORD_REGEX.matches(value)) return ValidationResult.Error("Password contains invalid characters. Only letters, numbers, and @#\$!%*?&_- are allowed")

        val missing = mutableListOf<String>()
        if (value.length < 8)                  missing.add("at least 8 characters")
        if (!value.any { it.isUpperCase() })   missing.add("one uppercase letter (A-Z)")
        if (!value.any { it.isLowerCase() })   missing.add("one lowercase letter (a-z)")
        if (!value.any { it.isDigit() })       missing.add("one number (0-9)")
        if (!value.any { it in SPECIAL_CHARS }) missing.add("one special character (@#\$!%*?&_-)")

        return if (missing.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error("Password needs: ${missing.joinToString(", ")}")
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
    // PHONE   (used by SignUpActivity + AuthViewModel)
    // Rules: not empty | digits only after stripping non-digits | length 10–13
    // ─────────────────────────────────────────────────────────────────────────
    fun validatePhone(value: String): ValidationResult {
        val digits = value.trim().replace(Regex("[^0-9]"), "")
        return when {
            value.trim().isEmpty() -> ValidationResult.Error("Phone number is required")
            digits.length < 10     -> ValidationResult.Error("Phone number must be at least 10 digits")
            digits.length > 13     -> ValidationResult.Error("Phone number must not exceed 13 digits")
            else                   -> ValidationResult.Success
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