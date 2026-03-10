package com.example.communitys.view.login

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.communitys.view.signup.SignUpActivity
import com.example.communitys.view.welcome.WelcomeActivity
import com.example.communitys.R
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
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
        val email = etEmail.text.toString()
        tilEmail.error = null
        
        tvForgotPassword.isEnabled = false
        viewModel.resetPassword(email)
        
        // Re-enable after a delay
        tvForgotPassword.postDelayed({
            tvForgotPassword.isEnabled = true
        }, 3000)
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
