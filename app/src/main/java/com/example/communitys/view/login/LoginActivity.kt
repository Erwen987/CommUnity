package com.example.communitys.view.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.communitys.view.signup.SignUpActivity
import com.example.communitys.view.welcome.WelcomeActivity
import com.example.communitys.R
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView

    private val authHelper = SupabaseAuthHelper()
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Check if user is already logged in
        if (authHelper.isUserLoggedIn()) {
            navigateToWelcome()
            return
        }

        // Initialize views
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUp = findViewById(R.id.tvSignUp)

        setupObservers()
        setupClickListeners()
    }

    private var forgotPasswordDialog: AlertDialog? = null

    private fun setupObservers() {
        // Observe validation errors
        viewModel.validationErrors.observe(this) { errors ->
            tilEmail.error = errors.emailError
            tilPassword.error = errors.passwordError

            // Focus on first error field
            when {
                errors.emailError != null -> etEmail.requestFocus()
                errors.passwordError != null -> etPassword.requestFocus()
            }
        }

        // Observe reset password state
        viewModel.resetPasswordState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    forgotPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                    forgotPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = "Sending..."
                }
                is AuthViewModel.AuthState.Success -> {
                    forgotPasswordDialog?.dismiss()
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Email Sent")
                        .setMessage("A password reset link has been sent to your email. Please check your inbox.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                is AuthViewModel.AuthState.Error -> {
                    forgotPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    forgotPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = "Send"
                    val tilEmail = forgotPasswordDialog?.findViewById<TextInputLayout>(R.id.tilForgotEmail)
                    tilEmail?.error = state.message
                }
            }
        }

        // Observe login state
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    btnLogin.isEnabled = false
                    btnLogin.text = "Logging in..."
                }
                is AuthViewModel.AuthState.Success -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN"
                    
                    // Save user name
                    saveUserName(etEmail.text.toString())
                    
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    navigateToWelcome()
                }
                is AuthViewModel.AuthState.Error -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN"
                    
                    // Show error on appropriate field
                    when {
                        state.message.contains("email", ignoreCase = true) -> {
                            tilEmail.error = state.message
                        }
                        state.message.contains("password", ignoreCase = true) -> {
                            tilPassword.error = state.message
                        }
                        else -> {
                            Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            viewModel.login(email, password)
        }

        tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Clear errors when user starts typing
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilEmail.error = null
        }

        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }
    }

    private fun handleForgotPassword() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val tilEmail = dialogView.findViewById<TextInputLayout>(R.id.tilForgotEmail)
        val etEmail  = dialogView.findViewById<TextInputEditText>(R.id.etForgotEmail)

        // Pre-fill email if already typed in login field
        val prefill = this.etEmail.text.toString()
        if (prefill.isNotEmpty()) etEmail.setText(prefill)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Send", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        forgotPasswordDialog = dialog

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            tilEmail.error = null
            viewModel.resetPassword(etEmail.text.toString())
        }

        dialog.setOnDismissListener { forgotPasswordDialog = null }
    }

    private fun navigateToWelcome() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveUserName(email: String) {
        val userName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val sharedPreferences = getSharedPreferences("CommUnityPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("userName", userName).apply()
    }
}
