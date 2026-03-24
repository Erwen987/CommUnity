package com.example.communitys.view.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.communitys.view.signup.SignUpActivity
import com.example.communitys.view.welcome.WelcomeActivity
import com.example.communitys.R
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

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

                    // state.email holds the resolved email (even if user typed a phone)
                    saveUserName(state.email ?: etEmail.text.toString())

                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    navigateToWelcome()
                }
                is AuthViewModel.AuthState.Error -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN"

                    val msg = state.message
                    when {
                        msg.contains("permanently banned", ignoreCase = true) ->
                            showBannedDialog(msg, canAppeal = false)
                        msg.contains("temporarily suspended", ignoreCase = true) ->
                            showInfoDialog("Account Suspended", msg)
                        msg.contains("deleted", ignoreCase = true) ->
                            showInfoDialog("Account Deleted", msg)
                        msg.contains("password", ignoreCase = true) ->
                            tilPassword.error = msg
                        msg.contains("portal", ignoreCase = true) ->
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        else ->
                            tilEmail.error = msg
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

    private fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBannedDialog(message: String, canAppeal: Boolean) {
        val email = etEmail.text.toString().trim()
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle("Account Permanently Banned")
            .setMessage(message)
            .setNegativeButton("Close", null)
        if (canAppeal) {
            builder.setPositiveButton("Submit Appeal") { _, _ ->
                showAppealDialog(email)
            }
        }
        builder.show()
    }

    private fun showAppealDialog(email: String) {
        val px = (resources.displayMetrics.density * 20).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(px, px / 2, px, 0)
        }
        val editText = android.widget.EditText(this).apply {
            hint = "Explain why this ban should be lifted..."
            minLines = 4
            maxLines = 8
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        container.addView(editText)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Submit Appeal")
            .setMessage("Describe why you believe this ban should be lifted. You can only submit one appeal.")
            .setView(container)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val reason = editText.text.toString().trim()
            if (reason.isBlank()) { editText.error = "Please describe your reason"; return@setOnClickListener }
            dialog.dismiss()
            lifecycleScope.launch {
                val result = viewModel.submitAppeal(email, reason)
                result.onSuccess {
                    MaterialAlertDialogBuilder(this@LoginActivity)
                        .setTitle("Appeal Submitted")
                        .setMessage("Your appeal has been submitted and will be reviewed by an admin. You will be notified at $email.")
                        .setPositiveButton("OK", null)
                        .show()
                }.onFailure { e ->
                    MaterialAlertDialogBuilder(this@LoginActivity)
                        .setTitle("Could Not Submit")
                        .setMessage(e.message ?: "Please try again later.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun navigateToWelcome() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveUserName(emailOrPhone: String) {
        val userName = if (emailOrPhone.contains("@")) {
            emailOrPhone.substringBefore("@").replaceFirstChar { it.uppercase() }
        } else {
            "User"
        }
        val sharedPreferences = getSharedPreferences("CommUnityPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("userName", userName).apply()
    }
}
