package com.example.communitys.view.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        setupPrivacyAgreement()
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

    // ── Privacy Agreement ─────────────────────────────────────────────────────

    private fun setupPrivacyAgreement() {
        val full   = "I have read and agree to the Privacy Policy"
        val link   = "Privacy Policy"
        val start  = full.indexOf(link)
        val end    = start + link.length
        val blue   = 0xFF1565C0.toInt()

        val spannable = SpannableString(full)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) { showPrivacyPolicyDialog() }
        }, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(UnderlineSpan(), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvPrivacyText.text           = spannable
        binding.tvPrivacyText.movementMethod = LinkMovementMethod.getInstance()

        // Hide error as soon as checkbox is checked
        binding.cbPrivacy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.tvPrivacyError.visibility = View.GONE
        }
    }

    private fun showPrivacyPolicyDialog() {
        val policy = """
PRIVACY POLICY — CommUnity App

Effective Date: January 1, 2026

1. INFORMATION WE COLLECT
We collect the following personal information when you register:
• Full name
• Email address
• Barangay of residence

When you use the app, we also collect:
• Issue reports you submit (problem type, description, photos, location)
• Document requests you make
• App usage data for service improvement

2. HOW WE USE YOUR INFORMATION
Your information is used to:
• Provide barangay community services
• Process your document requests and issue reports
• Notify you of updates on your submissions
• Improve the CommUnity app and services

3. WHO CAN SEE YOUR DATA
• Your issue reports and document requests are visible to authorized barangay officials only.
• Your personal information is never sold or shared with third parties outside the barangay.
• Other residents cannot view your personal information.

4. DATA SECURITY
We protect your data using secure servers provided by Supabase. Access is restricted to authorized personnel only.

5. YOUR RIGHTS
You have the right to:
• View and edit your profile information at any time
• Delete your account, which permanently removes your data from our system
• Request a copy of your stored data by contacting your barangay office

6. CHILDREN'S PRIVACY
CommUnity is intended for residents 18 years and older. We do not knowingly collect information from minors.

7. CHANGES TO THIS POLICY
We may update this Privacy Policy from time to time. Changes will be reflected in the app.

8. CONTACT US
For questions about this Privacy Policy, please contact your local Barangay Hall in Dagupan City, Pangasinan.

By registering, you confirm that you have read, understood, and agreed to this Privacy Policy.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage(policy)
            .setPositiveButton("I Understand") { dialog, _ ->
                binding.cbPrivacy.isChecked = true
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
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

        // ── Phone ─────────────────────────────────────────────────────────────
        binding.etPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                touchedFields.add("phone")
                validatePhoneField()
            } else {
                clearError(binding.tilPhone)
            }
        }
        binding.etPhone.addTextChangedListener(afterChanged {
            if ("phone" in touchedFields) validatePhoneField()
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

    private fun validatePhoneField() {
        val error = ValidationHelper.validatePhone(
            binding.etPhone.text.toString()
        ).errorMessage()
        setError(binding.tilPhone, error)
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
            listOf("firstName", "lastName", "barangay", "email", "phone", "password", "confirmPassword")
        )
        validateFirstNameField()
        validateLastNameField()
        validateBarangayField()
        validateEmailField()
        validatePhoneField()
        validatePasswordField()
        validateConfirmPasswordField()

        val fieldsValid = listOf(
            binding.tilFirstName.error,
            binding.tilLastName.error,
            binding.tilBarangay.error,
            binding.tilEmail.error,
            binding.tilPhone.error,
            binding.tilPassword.error,
            binding.tilConfirmPassword.error
        ).all { it == null }

        val privacyAccepted = binding.cbPrivacy.isChecked
        binding.tvPrivacyError.visibility = if (privacyAccepted) View.GONE else View.VISIBLE

        return fieldsValid && privacyAccepted
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
                    phone           = binding.etPhone.text.toString().trim(),
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