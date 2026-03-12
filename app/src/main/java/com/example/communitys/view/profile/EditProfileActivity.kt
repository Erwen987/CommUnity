package com.example.communitys.view.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.communitys.databinding.ActivityEditProfileBinding
import com.example.communitys.model.repository.AuthRepository
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill fields with data passed from ProfileFragment
        binding.etEditName.setText(intent.getStringExtra("name") ?: "")
        binding.etEditEmail.setText(intent.getStringExtra("email") ?: "")
        binding.etEditPhone.setText(intent.getStringExtra("phone") ?: "")

        // Strip city/province suffix added by formatBarangay() in ViewModel
        val rawBarangay = intent.getStringExtra("barangay") ?: ""
        val cleanBarangay = rawBarangay
            .removeSuffix(", Dagupan City, Pangasinan")
            .removePrefix("Barangay ")
            .trim()
        binding.etEditBarangay.setText(cleanBarangay)

        setupClickListeners()
    }

    private fun setupClickListeners() {

        binding.fabChangePhoto.setOnClickListener {
            Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelEdit.setOnClickListener { finish() }

        binding.btnSaveChanges.setOnClickListener {
            val fullName = binding.etEditName.text.toString().trim()
            val phone    = binding.etEditPhone.text.toString().trim()
            val barangay = binding.etEditBarangay.text.toString().trim()

            binding.tilEditName.error     = null
            binding.tilEditBarangay.error = null

            if (fullName.isEmpty()) {
                binding.tilEditName.error = "Name is required"
                return@setOnClickListener
            }
            if (barangay.isEmpty()) {
                binding.tilEditBarangay.error = "Barangay is required"
                return@setOnClickListener
            }

            // Split "First Last" → firstName + lastName
            val parts     = fullName.split(" ", limit = 2)
            val firstName = parts[0]
            val lastName  = if (parts.size > 1) parts[1] else ""

            binding.btnSaveChanges.isEnabled = false
            binding.btnSaveChanges.text      = "Saving…"

            lifecycleScope.launch {
                authRepository.updateUserProfile(firstName, lastName, barangay, phone)
                    .onSuccess {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .onFailure { e ->
                        binding.btnSaveChanges.isEnabled = true
                        binding.btnSaveChanges.text      = "Save Changes"
                        Toast.makeText(
                            this@EditProfileActivity,
                            e.message ?: "Failed to save changes",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }
}
