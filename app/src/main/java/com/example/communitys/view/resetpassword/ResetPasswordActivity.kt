package com.example.communitys.view.resetpassword

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.communitys.R
import com.example.communitys.model.repository.AuthRepository
import com.example.communitys.utils.ValidationHelper
import com.example.communitys.view.login.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Parse the deep link URI: communitys://reset-password#access_token=...&refresh_token=...
        val uri = intent?.data
        if (uri == null) {
            Toast.makeText(this, "Invalid reset link.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        val fragment = uri.fragment ?: uri.encodedQuery
        if (fragment.isNullOrBlank()) {
            Toast.makeText(this, "Invalid or expired reset link.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        // Parse fragment params: access_token=xxx&refresh_token=xxx&type=recovery...
        val params = fragment.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()

        val accessToken  = params["access_token"]
        val refreshToken = params["refresh_token"]
        val type         = params["type"]

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank() || type != "recovery") {
            Toast.makeText(this, "Invalid or expired reset link.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        val tilNew     = findViewById<TextInputLayout>(R.id.tilNewPassword)
        val etNew      = findViewById<TextInputEditText>(R.id.etNewPassword)
        val tilConfirm = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val etConfirm  = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnReset   = findViewById<MaterialButton>(R.id.btnResetPassword)

        btnReset.setOnClickListener {
            val newPw     = etNew.text.toString()
            val confirmPw = etConfirm.text.toString()
            tilNew.error     = null
            tilConfirm.error = null

            with(ValidationHelper) {
                val pwResult = validatePassword(newPw)
                if (pwResult is ValidationHelper.ValidationResult.Error) {
                    tilNew.error = pwResult.message
                    return@setOnClickListener
                }
                val confirmResult = validateConfirmPassword(newPw, confirmPw)
                if (confirmResult is ValidationHelper.ValidationResult.Error) {
                    tilConfirm.error = confirmResult.message
                    return@setOnClickListener
                }
            }

            btnReset.isEnabled = false
            btnReset.text = "Resetting..."

            lifecycleScope.launch {
                val result = authRepository.updatePasswordWithSession(accessToken, refreshToken, newPw)
                result.onSuccess {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Password reset successfully! Please log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToLogin()
                }.onFailure { e ->
                    btnReset.isEnabled = true
                    btnReset.text = "RESET PASSWORD"
                    Toast.makeText(this@ResetPasswordActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
