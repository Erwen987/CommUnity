package com.example.communitys.view.reportissue

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.communitys.R
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.SupabaseStorageHelper
import com.example.communitys.databinding.ActivityReportIssueBinding
import com.example.communitys.model.data.ReportModel
import com.example.communitys.model.repository.ReportRepository
import kotlinx.coroutines.launch

class ReportIssueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportIssueBinding
    private val storageHelper = SupabaseStorageHelper()
    private val authHelper = SupabaseAuthHelper()
    private val reportRepository = ReportRepository()

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Show preview
            binding.ivPreview.setImageURI(it)
            binding.ivPreview.visibility = android.view.View.VISIBLE
            binding.ivUploadIcon.visibility = android.view.View.GONE
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportIssueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Upload image
        binding.cvUploadImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // Map location
        binding.cvMap.setOnClickListener {
            // TODO: Implement map picker
            Toast.makeText(this, "Map selection coming soon", Toast.LENGTH_SHORT).show()
        }

        // Submit report
        binding.btnSubmitReport.setOnClickListener {
            submitReport()
        }

        // Clear errors when user starts typing
        binding.etProblem.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilProblem.error = null
        }

        binding.etDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilDescription.error = null
        }

        binding.etProblem.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilProblem.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.etDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilDescription.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun submitReport() {
        val problem = binding.etProblem.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Clear previous errors
        binding.tilProblem.error = null
        binding.tilDescription.error = null

        var hasError = false

        // Validation
        when {
            problem.isEmpty() -> {
                binding.tilProblem.error = "Problem is required"
                binding.etProblem.requestFocus()
                hasError = true
            }
            problem.length < 3 -> {
                binding.tilProblem.error = "Problem must be at least 3 characters"
                binding.etProblem.requestFocus()
                hasError = true
            }
        }

        when {
            description.isEmpty() -> {
                binding.tilDescription.error = "Description is required"
                if (!hasError) binding.etDescription.requestFocus()
                hasError = true
            }
            description.length < 10 -> {
                binding.tilDescription.error = "Description must be at least 10 characters"
                if (!hasError) binding.etDescription.requestFocus()
                hasError = true
            }
        }

        if (hasError) return

        // Show loading
        binding.btnSubmitReport.isEnabled = false
        binding.btnSubmitReport.text = "Submitting..."

        lifecycleScope.launch {
            try {
                // Get user ID
                val userId = authHelper.getCurrentUserId()
                if (userId == null) {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        "User not logged in",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnSubmitReport.isEnabled = true
                    binding.btnSubmitReport.text = "Submit Report"
                    return@launch
                }

                // Upload image if selected
                if (selectedImageUri != null) {
                    val uploadResult = storageHelper.uploadImage(
                        bucketName = "issue-images",
                        fileUri = selectedImageUri!!,
                        userId = userId,
                        context = this@ReportIssueActivity
                    )

                    if (uploadResult.isSuccess) {
                        uploadedImageUrl = uploadResult.getOrNull()
                    } else {
                        Toast.makeText(
                            this@ReportIssueActivity,
                            "Failed to upload image: ${uploadResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnSubmitReport.isEnabled = true
                        binding.btnSubmitReport.text = "Submit Report"
                        return@launch
                    }
                }

                // Create report object
                val report = ReportModel(
                    userId = userId,
                    problem = problem,
                    description = description,
                    imageUrl = uploadedImageUrl,
                    locationLat = null, // TODO: Add location when map is implemented
                    locationLng = null
                )

                // Save to database
                val saveResult = reportRepository.createReport(report)

                if (saveResult.isSuccess) {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        "Report submitted successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        "Failed to save report: ${saveResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnSubmitReport.isEnabled = true
                    binding.btnSubmitReport.text = "Submit Report"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ReportIssueActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnSubmitReport.isEnabled = true
                binding.btnSubmitReport.text = "Submit Report"
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

}