package com.example.communitys.view.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.communitys.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill fields with data passed from ProfileFragment
        binding.etEditName.setText(intent.getStringExtra("name") ?: "")
        binding.etEditEmail.setText(intent.getStringExtra("email") ?: "")
        binding.etEditPhone.setText(intent.getStringExtra("phone") ?: "")
        binding.etEditBarangay.setText(intent.getStringExtra("barangay") ?: "")

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // + Photo button
        binding.fabChangePhoto.setOnClickListener {
            Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show()
        }

        // Cancel — just go back
        binding.btnCancelEdit.setOnClickListener {
            finish()
        }

        // Save Changes — validate then send data back to ProfileFragment
        binding.btnSaveChanges.setOnClickListener {
            val name     = binding.etEditName.text.toString().trim()
            val email    = binding.etEditEmail.text.toString().trim()
            val phone    = binding.etEditPhone.text.toString().trim()
            val barangay = binding.etEditBarangay.text.toString().trim()

            // Basic validation
            if (name.isEmpty()) {
                binding.tilEditName.error = "Name is required"
                return@setOnClickListener
            }
            binding.tilEditName.error = null

            // TODO: Call ViewModel to save to Supabase here
            // viewModel.updateProfile(name, email, phone, barangay)

            // Send updated data back to ProfileFragment
            val result = Intent().apply {
                putExtra("name",     name)
                putExtra("email",    email)
                putExtra("phone",    phone)
                putExtra("barangay", barangay)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}