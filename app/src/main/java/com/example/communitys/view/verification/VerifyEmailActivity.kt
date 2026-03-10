package com.example.communitys.view.verification

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.communitys.databinding.ActivityVerifyEmailBinding
import com.example.communitys.model.repository.AuthRepository
import com.example.communitys.view.login.LoginActivity
import kotlinx.coroutines.launch

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyEmailBinding
    private val authRepository = AuthRepository()

    private lateinit var email: String
    private var firstName: String = ""
    private var lastName: String = ""
    private var barangay: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Receive all user details from SignUpActivity
        email     = intent.getStringExtra("email")     ?: ""
        firstName = intent.getStringExtra("firstName") ?: ""
        lastName  = intent.getStringExtra("lastName")  ?: ""
        barangay  = intent.getStringExtra("barangay")  ?: ""

        binding.tvEmail.text = email

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnVerify.setOnClickListener {
            verifyOTP()
        }

        binding.tvResendOTP.setOnClickListener {
            resendOTP()
        }

        binding.etOTP.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilOTP.error = null
        }

        binding.etOTP.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilOTP.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun verifyOTP() {
        val otp = binding.etOTP.text.toString().trim()

        binding.tilOTP.error = null

        when {
            otp.isEmpty() -> {
                binding.tilOTP.error = "Please enter OTP"
                binding.etOTP.requestFocus()
                return
            }
            otp.length < 6 -> {
                binding.tilOTP.error = "OTP must be 6 digits"
                binding.etOTP.requestFocus()
                return
            }
        }

        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = "Verifying..."

        lifecycleScope.launch {
            // ✅ Pass firstName, lastName, barangay so profile gets saved
            val result = authRepository.verifyEmail(
                email     = email,
                otp       = otp,
                firstName = firstName,
                lastName  = lastName,
                barangay  = barangay
            )

            result.onSuccess {
                Toast.makeText(
                    this@VerifyEmailActivity,
                    "Email verified! You can now log in.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this@VerifyEmailActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { error ->
                val errorMessage = error.message ?: "Verification failed"
                when {
                    errorMessage.contains("invalid", ignoreCase = true) ||
                            errorMessage.contains("incorrect", ignoreCase = true) ||
                            errorMessage.contains("wrong", ignoreCase = true) ->
                        binding.tilOTP.error = "Invalid OTP code"
                    errorMessage.contains("expired", ignoreCase = true) ->
                        binding.tilOTP.error = "OTP has expired. Tap resend."
                    else ->
                        Toast.makeText(this@VerifyEmailActivity, errorMessage, Toast.LENGTH_LONG).show()
                }

                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "VERIFY EMAIL"
            }
        }
    }

    private fun resendOTP() {
        binding.tvResendOTP.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.resendOTP(email)

            result.onSuccess {
                Toast.makeText(this@VerifyEmailActivity, "OTP resent to $email", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@VerifyEmailActivity, "Failed to resend OTP", Toast.LENGTH_SHORT).show()
            }

            binding.tvResendOTP.isEnabled = true
        }
    }
}