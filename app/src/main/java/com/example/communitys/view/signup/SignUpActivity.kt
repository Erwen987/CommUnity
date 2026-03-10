package com.example.communitys.view.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.communitys.databinding.ActivitySignupBinding
import com.example.communitys.utils.ValidationHelper
import com.example.communitys.utils.ValidationHelper.errorMessage
import com.example.communitys.view.login.LoginActivity
import com.example.communitys.view.verification.VerifyEmailActivity
import com.example.communitys.viewmodel.AuthViewModel

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var viewModel: AuthViewModel

    // Track which fields have been "touched" (lost focus at least once)
    // Errors only show on fields the user has already interacted with
    private val touchedFields = mutableSetOf<String>()

    private val barangayList = listOf(
        "Bacayao Norte", "Bacayao Sur", "Barangay I (Pob.)", "Barangay II (Pob.)",
        "Barangay III (Pob.)", "Barangay IV (Pob.)", "Bolosan", "Bonuan Binloc",
        "Bonuan Boquig", "Bonuan Gueset", "Calmay", "Carael", "Caranglaan",
        "Herrero", "Lasip Chico", "Lasip Grande", "Lomboy", "Lucao", "Malued",
        "Mamalingling", "Mangin", "Mayombo", "Pantal", "Poblacion Oeste",
        "Pogo Chico", "Pogo Grande", "Pugaro Suit", "Quezon", "San Jose",
        "San Lázaro", "Salapingao", "Taloy", "Tebeng"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        setupBarangayDropdown()
        setupValidation()
        setupClickListeners()
        observeViewModel()
    }

    // ── Observe signup state ──────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.signupState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnSignUp.isEnabled = false
                    binding.btnSignUp.text = "Creating account..."
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "SIGN UP"
                    Toast.makeText(this, "Account created! Check your email for the OTP code.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, VerifyEmailActivity::class.java).apply {
                        putExtra("email",     binding.etEmail.text.toString().trim())
                        putExtra("firstName", binding.etFirstName.text.toString().trim())
                        putExtra("lastName",  binding.etLastName.text.toString().trim())
                        putExtra("barangay",  binding.actvBarangay.text.toString().trim())
                        putExtra("password",  binding.etPassword.text.toString())
                    }
                    startActivity(intent)
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "SIGN UP"
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Barangay dropdown ─────────────────────────────────────────────────────

    private fun setupBarangayDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, barangayList)
        binding.actvBarangay.setAdapter(adapter)

        binding.actvBarangay.setOnItemClickListener { _, _, _, _ ->
            touchedFields.add("barangay")
            validateBarangayField()
        }
    }

    // ── Validation — errors show on focus lost only ───────────────────────────

    private fun setupValidation() {

        // ── First Name ────────────────────────────────────────────────────────
        binding.etFirstName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                touchedFields.add("firstName")
                validateFirstNameField()
            } else {
                clearError(binding.tilFirstName)
            }
        }
        binding.etFirstName.addTextChangedListener(afterChanged {
            if ("firstName" in touchedFields) validateFirstNameField()
        })

        // ── Last Name ─────────────────────────────────────────────────────────
        binding.etLastName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                touchedFields.add("lastName")
                validateLastNameField()
            } else {
                clearError(binding.tilLastName)
            }
        }
        binding.etLastName.addTextChangedListener(afterChanged {
            if ("lastName" in touchedFields) validateLastNameField()
        })

        // ── Email ─────────────────────────────────────────────────────────────
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                touchedFields.add("email")
                validateEmailField()
            } else {
                clearError(binding.tilEmail)
            }
        }
        binding.etEmail.addTextChangedListener(afterChanged {
            if ("email" in touchedFields) validateEmailField()
        })

        // ── Password ──────────────────────────────────────────────────────────
        // KEY FIX: No live error while typing — only validates after focus leaves
        // This keeps the eye icon (password_toggle) always visible and clickable
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                touchedFields.add("password")
                validatePasswordField()
                if ("confirmPassword" in touchedFields) validateConfirmPasswordField()
            } else {
                // When tapping into password field — clear error so eye icon shows
                clearError(binding.tilPassword)
            }
        }
        binding.etPassword.addTextChangedListener(afterChanged {
            // Only re-validate after focus has left the field
            if ("password" in touchedFields && !binding.etPassword.hasFocus()) {
                validatePasswordField()
            }
            if ("confirmPassword" in touchedFields) validateConfirmPasswordField()
        })

        // ── Confirm Password ──────────────────────────────────────────────────
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                touchedFields.add("confirmPassword")
                validateConfirmPasswordField()
            } else {
                clearError(binding.tilConfirmPassword)
            }
        }
        binding.etConfirmPassword.addTextChangedListener(afterChanged {
            if ("confirmPassword" in touchedFields && !binding.etConfirmPassword.hasFocus()) {
                validateConfirmPasswordField()
            }
        })
    }

    // ── Individual field validators ───────────────────────────────────────────

    private fun validateFirstNameField() {
        val error = ValidationHelper.validateName(
            binding.etFirstName.text.toString(), "First name"
        ).errorMessage()
        setError(binding.tilFirstName, error)
    }

    private fun validateLastNameField() {
        val error = ValidationHelper.validateName(
            binding.etLastName.text.toString(), "Last name"
        ).errorMessage()
        setError(binding.tilLastName, error)
    }

    private fun validateBarangayField() {
        val error = ValidationHelper.validateBarangay(
            binding.actvBarangay.text.toString()
        ).errorMessage()
        setError(binding.tilBarangay, error)
    }

    private fun validateEmailField() {
        val error = ValidationHelper.validateEmail(
            binding.etEmail.text.toString()
        ).errorMessage()
        setError(binding.tilEmail, error)
    }

    private fun validatePasswordField() {
        val error = ValidationHelper.validatePassword(
            binding.etPassword.text.toString()
        ).errorMessage()
        setError(binding.tilPassword, error)
    }

    private fun validateConfirmPasswordField() {
        val error = ValidationHelper.validateConfirmPassword(
            binding.etPassword.text.toString(),
            binding.etConfirmPassword.text.toString()
        ).errorMessage()
        setError(binding.tilConfirmPassword, error)
    }

    // ── Error helpers ─────────────────────────────────────────────────────────

    private fun setError(
        til: com.google.android.material.textfield.TextInputLayout,
        error: String?
    ) {
        til.error = error
        til.isErrorEnabled = error != null
    }

    private fun clearError(
        til: com.google.android.material.textfield.TextInputLayout
    ) {
        til.error = null
        til.isErrorEnabled = false
    }

    // ── Full validation on submit (marks all fields as touched) ───────────────

    private fun validateAll(): Boolean {
        touchedFields.addAll(
            listOf("firstName", "lastName", "barangay", "email", "password", "confirmPassword")
        )
        validateFirstNameField()
        validateLastNameField()
        validateBarangayField()
        validateEmailField()
        validatePasswordField()
        validateConfirmPasswordField()

        return listOf(
            binding.tilFirstName.error,
            binding.tilLastName.error,
            binding.tilBarangay.error,
            binding.tilEmail.error,
            binding.tilPassword.error,
            binding.tilConfirmPassword.error
        ).all { it == null }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            if (validateAll()) {
                viewModel.signUp(
                    firstName       = binding.etFirstName.text.toString().trim(),
                    lastName        = binding.etLastName.text.toString().trim(),
                    barangay        = binding.actvBarangay.text.toString().trim(),
                    email           = binding.etEmail.text.toString().trim(),
                    password        = binding.etPassword.text.toString(),
                    confirmPassword = binding.etConfirmPassword.text.toString()
                )
            }
        }

        binding.tvLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // ── TextWatcher helper ────────────────────────────────────────────────────

    private fun afterChanged(block: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { block(s.toString()) }
    }
}