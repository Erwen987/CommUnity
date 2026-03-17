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

    fun login(email: String, password: String) {
        // Validate inputs
        val emailValidation = ValidationHelper.validateEmail(email)
        val passwordValidation = ValidationHelper.validatePassword(password)

        val errors = ValidationErrors()
        
        if (emailValidation is ValidationHelper.ValidationResult.Error) {
            errors.emailError = emailValidation.message
        }
        
        if (passwordValidation is ValidationHelper.ValidationResult.Error) {
            errors.passwordError = passwordValidation.message
        }

        if (errors.hasErrors()) {
            _validationErrors.value = errors
            return
        }

        // Clear errors and start loading
        _validationErrors.value = ValidationErrors()
        _loginState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.signIn(email.trim(), password)
            
            result.onSuccess {
                _loginState.value = AuthState.Success("Login successful!")
            }.onFailure { exception ->
                _loginState.value = AuthState.Error(exception.message ?: "Login failed")
            }
        }
    }

    fun signUp(
        firstName: String,
        lastName: String,
        barangay: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        // Validate all inputs
        val firstNameValidation = ValidationHelper.validateName(firstName, "First name")
        val lastNameValidation = ValidationHelper.validateName(lastName, "Last name")
        val barangayValidation = ValidationHelper.validateBarangay(barangay)
        val emailValidation = ValidationHelper.validateEmail(email)
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

        // Clear errors and start loading
        _validationErrors.value = ValidationErrors()
        _signupState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.signUp(
                email = email.trim(),
                password = password,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                barangay = barangay
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
        var passwordError: String? = null,
        var confirmPasswordError: String? = null
    ) {
        fun hasErrors(): Boolean {
            return firstNameError != null || lastNameError != null || 
                   barangayError != null || emailError != null || 
                   passwordError != null || confirmPasswordError != null
        }
    }
}
