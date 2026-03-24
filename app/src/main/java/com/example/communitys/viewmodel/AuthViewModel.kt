package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.model.repository.AuthRepository
import com.example.communitys.utils.ValidationHelper
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // LiveData for login state
    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState

    // LiveData for signup state
    private val _signupState = MutableLiveData<AuthState>()
    val signupState: LiveData<AuthState> = _signupState

    // LiveData for validation errors
    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    // LiveData for forgot password state
    private val _resetPasswordState = MutableLiveData<AuthState>()
    val resetPasswordState: LiveData<AuthState> = _resetPasswordState

    fun login(emailOrPhone: String, password: String) {
        val input = emailOrPhone.trim()

        // Basic empty checks
        val errors = ValidationErrors()
        if (input.isEmpty()) {
            errors.emailError = "Email or phone number is required"
        }
        if (password.isEmpty()) {
            errors.passwordError = "Password is required"
        }
        if (errors.hasErrors()) {
            _validationErrors.value = errors
            return
        }

        // Detect if input is a phone number (only digits, +, spaces, dashes after trim)
        val isPhone = input.replace(Regex("[^0-9]"), "").length >= 10 &&
                      input.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }

        // If email: validate format; if phone: validate phone format
        if (!isPhone) {
            val emailValidation = ValidationHelper.validateEmail(input)
            if (emailValidation is ValidationHelper.ValidationResult.Error) {
                errors.emailError = emailValidation.message
            }
        } else {
            val phoneValidation = ValidationHelper.validatePhone(input)
            if (phoneValidation is ValidationHelper.ValidationResult.Error) {
                errors.emailError = phoneValidation.message
            }
        }
        if (errors.hasErrors()) {
            _validationErrors.value = errors
            return
        }

        _validationErrors.value = ValidationErrors()
        _loginState.value = AuthState.Loading

        viewModelScope.launch {
            // Check maintenance mode before anything else
            if (authRepository.isMaintenanceModeOn()) {
                _loginState.value = AuthState.Error(
                    "The system is currently under maintenance. Please try again later."
                )
                return@launch
            }

            // If phone, look up the email first
            val resolvedEmail: String = if (isPhone) {
                val lookup = authRepository.lookupEmailByPhone(input)
                if (lookup.isFailure) {
                    _loginState.value = AuthState.Error(
                        lookup.exceptionOrNull()?.message ?: "No account found with this phone number."
                    )
                    return@launch
                }
                lookup.getOrThrow()
            } else {
                input
            }

            // Block admin/official accounts
            val blockedEmails = listOf(
                "pandahuntergamer09@gmail.com",
                "jerwenbacani80@gmail.com"
            )
            if (resolvedEmail.lowercase() in blockedEmails) {
                _loginState.value = AuthState.Error(
                    "This account is for the admin/official portal only. Please use the web portal to log in."
                )
                return@launch
            }

            val result = authRepository.signIn(resolvedEmail, password)
            result.onSuccess {
                _loginState.value = AuthState.Success("Login successful!", resolvedEmail)
            }.onFailure { exception ->
                _loginState.value = AuthState.Error(exception.message ?: "Login failed")
            }
        }
    }

    suspend fun submitAppeal(email: String, reason: String): Result<Unit> {
        return authRepository.submitAppeal(email, reason)
    }

    fun signUp(
        firstName: String,
        lastName: String,
        barangay: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        // Validate all inputs
        val firstNameValidation = ValidationHelper.validateName(firstName, "First name")
        val lastNameValidation = ValidationHelper.validateName(lastName, "Last name")
        val barangayValidation = ValidationHelper.validateBarangay(barangay)
        val emailValidation = ValidationHelper.validateEmail(email)
        val phoneValidation = ValidationHelper.validatePhone(phone)
        val passwordValidation = ValidationHelper.validatePassword(password)

        val errors = ValidationErrors()

        if (firstNameValidation is ValidationHelper.ValidationResult.Error) {
            errors.firstNameError = firstNameValidation.message
        }

        if (lastNameValidation is ValidationHelper.ValidationResult.Error) {
            errors.lastNameError = lastNameValidation.message
        }

        if (barangayValidation is ValidationHelper.ValidationResult.Error) {
            errors.barangayError = barangayValidation.message
        }

        if (emailValidation is ValidationHelper.ValidationResult.Error) {
            errors.emailError = emailValidation.message
        }

        if (phoneValidation is ValidationHelper.ValidationResult.Error) {
            errors.phoneError = phoneValidation.message
        }

        if (passwordValidation is ValidationHelper.ValidationResult.Error) {
            errors.passwordError = passwordValidation.message
        }

        if (confirmPassword.isEmpty()) {
            errors.confirmPasswordError = "Please confirm your password"
        } else if (password != confirmPassword) {
            errors.confirmPasswordError = "Passwords do not match"
        }

        if (errors.hasErrors()) {
            _validationErrors.value = errors
            return
        }

        // Block admin and official accounts from registering in the resident app
        val blockedEmails = listOf(
            "pandahuntergamer09@gmail.com",
            "jerwenbacani80@gmail.com"
        )
        if (email.trim().lowercase() in blockedEmails) {
            val errors2 = ValidationErrors()
            errors2.emailError = "This email is reserved for the admin/official portal."
            _validationErrors.value = errors2
            return
        }

        // Clear errors and start loading
        _validationErrors.value = ValidationErrors()
        _signupState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.signUp(
                email = email.trim(),
                password = password,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                barangay = barangay,
                phone = phone.trim()
            )

            result.onSuccess { message ->
                _signupState.value = AuthState.Success(message, email)
            }.onFailure { exception ->
                _signupState.value = AuthState.Error(exception.message ?: "Sign up failed")
            }
        }
    }

    fun resetPassword(email: String) {
        val emailValidation = ValidationHelper.validateEmail(email)

        if (emailValidation is ValidationHelper.ValidationResult.Error) {
            _resetPasswordState.value = AuthState.Error(emailValidation.message)
            return
        }

        _resetPasswordState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.resetPassword(email.trim())

            result.onSuccess {
                _resetPasswordState.value = AuthState.Success("Password reset email sent! Please check your inbox.")
            }.onFailure { exception ->
                _resetPasswordState.value = AuthState.Error(exception.message ?: "Failed to send reset email")
            }
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val message: String, val email: String? = null) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    data class ValidationErrors(
        var firstNameError: String? = null,
        var lastNameError: String? = null,
        var barangayError: String? = null,
        var emailError: String? = null,
        var phoneError: String? = null,
        var passwordError: String? = null,
        var confirmPasswordError: String? = null
    ) {
        fun hasErrors(): Boolean {
            return firstNameError != null || lastNameError != null ||
                   barangayError != null || emailError != null ||
                   phoneError != null || passwordError != null ||
                   confirmPasswordError != null
        }
    }
}
