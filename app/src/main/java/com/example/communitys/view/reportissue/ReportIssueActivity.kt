package com.example.communitys.view.reportissue

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
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
    private val storageHelper    = SupabaseStorageHelper()
    private val authHelper       = SupabaseAuthHelper()
    private val reportRepository = ReportRepository()

    private var selectedImageUri: Uri?    = null
    private var uploadedImageUrl: String? = null
    private var capturedLat: Double?      = null
    private var capturedLng: Double?      = null

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    // Problem options — "Others" must stay last
    private val problemOptions = listOf(
        "Broken Road / Pothole",
        "Broken Street Light",
        "Clogged Drainage / Flood",
        "Illegal Dumping / Garbage",
        "Damaged Bridge / Footpath",
        "Broken Water Pipe / No Water Supply",
        "Stray Animals",
        "Noise Complaint",
        "Illegal Construction",
        "Others"
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivPreview.setImageURI(it)
            binding.ivPreview.visibility    = View.VISIBLE
            binding.ivUploadIcon.visibility = View.GONE
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openImagePicker()
        else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationCapture()
        } else {
            setLocationStatus("Location permission denied", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportIssueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupProblemDropdown()
        setupClickListeners()
        requestLocationCapture()
    }

    // ── Location auto-capture ─────────────────────────────────────────────────

    private fun requestLocationCapture() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            startLocationCapture()
        } else {
            setLocationStatus("Requesting location permission...", null)
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationCapture() {
        setLocationStatus("Getting your location...", null)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            // Try last known first for instant result
            val lastKnown =
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnown != null) {
                onLocationCaptured(lastKnown)
                return
            }

            // No cached — request fresh fix
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocationCaptured(location)
                    stopLocationUpdates()
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (isGpsEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0L, 0f, locationListener!!
                )
            }
            if (isNetEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener!!
                )
            }

            if (!isGpsEnabled && !isNetEnabled) {
                setLocationStatus("Enable GPS to attach location", false)
                return
            }

            // Timeout after 15 seconds
            binding.root.postDelayed({
                if (capturedLat == null) {
                    stopLocationUpdates()
                    setLocationStatus("Could not get location", false)
                }
            }, 15_000)

        } catch (e: SecurityException) {
            setLocationStatus("Location unavailable", false)
        }
    }

    private fun onLocationCaptured(location: Location) {
        capturedLat = location.latitude
        capturedLng = location.longitude
        setLocationStatus("Location captured", true)
        binding.tvMapCoords.text = "%.5f, %.5f".format(location.latitude, location.longitude)
        binding.tvMapCoords.visibility = View.VISIBLE
    }

    private fun setLocationStatus(message: String, success: Boolean?) {
        binding.tvMapStatus.text = message
        when (success) {
            true  -> {
                binding.tvMapStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
                binding.ivMapPin.setColorFilter(ContextCompat.getColor(this, R.color.primary_blue))
            }
            false -> {
                binding.tvMapStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                binding.ivMapPin.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
            null  -> {
                binding.tvMapStatus.setTextColor(ContextCompat.getColor(this, R.color.hint_text_color))
                binding.ivMapPin.setColorFilter(ContextCompat.getColor(this, R.color.hint_text_color))
            }
        }
    }

    private fun stopLocationUpdates() {
        locationListener?.let { locationManager?.removeUpdates(it) }
        locationListener = null
    }

    // ── Problem dropdown ──────────────────────────────────────────────────────

    private fun setupProblemDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, problemOptions)
        binding.actvProblem.setAdapter(adapter)

        binding.actvProblem.setOnItemClickListener { _, _, position, _ ->
            binding.tilProblem.error = null
            val isOthers = problemOptions[position] == "Others"
            val visibility = if (isOthers) View.VISIBLE else View.GONE
            binding.tvDescriptionLabel.visibility = visibility
            binding.tilDescription.visibility     = visibility
            if (!isOthers) binding.etDescription.setText("")
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.cvUploadImage.setOnClickListener { checkPermissionAndPickImage() }
        binding.btnSubmitReport.setOnClickListener { submitReport() }
        binding.etDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilDescription.error = null
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private fun submitReport() {
        val selectedProblem = binding.actvProblem.text.toString().trim()
        val description     = binding.etDescription.text.toString().trim()
        val isOthers        = selectedProblem == "Others"

        binding.tilProblem.error     = null
        binding.tilDescription.error = null

        if (selectedProblem.isEmpty() || !problemOptions.contains(selectedProblem)) {
            binding.tilProblem.error = "Please select a problem"
            return
        }

        if (isOthers) {
            when {
                description.isEmpty() -> {
                    binding.tilDescription.error = "Please describe the issue"
                    binding.etDescription.requestFocus()
                    return
                }
                description.length < 10 -> {
                    binding.tilDescription.error = "Description must be at least 10 characters"
                    binding.etDescription.requestFocus()
                    return
                }
            }
        }

        val finalDescription = if (isOthers) description else selectedProblem

        binding.btnSubmitReport.isEnabled = false
        binding.btnSubmitReport.text      = "Submitting..."

        lifecycleScope.launch {
            try {
                val userId = authHelper.getCurrentUserId()
                if (userId == null) {
                    Toast.makeText(this@ReportIssueActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    resetButton()
                    return@launch
                }

                val barangay = reportRepository.getUserBarangay(userId)

                if (selectedImageUri != null) {
                    val uploadResult = storageHelper.uploadImage(
                        bucketName = "issue-images",
                        fileUri    = selectedImageUri!!,
                        userId     = userId,
                        context    = this@ReportIssueActivity
                    )
                    if (uploadResult.isSuccess) {
                        uploadedImageUrl = uploadResult.getOrNull()
                    } else {
                        Toast.makeText(
                            this@ReportIssueActivity,
                            "Failed to upload image: ${uploadResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        resetButton()
                        return@launch
                    }
                }

                val report = ReportModel(
                    userId      = userId,
                    problem     = selectedProblem,
                    description = finalDescription,
                    imageUrl    = uploadedImageUrl,
                    locationLat = capturedLat,
                    locationLng = capturedLng,
                    barangay    = barangay
                )

                val saveResult = reportRepository.createReport(report)

                if (saveResult.isSuccess) {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        "Report submitted! Points will be awarded once officials review it.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ReportIssueActivity,
                        "Failed to save report: ${saveResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetButton()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReportIssueActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                resetButton()
            }
        }
    }

    private fun resetButton() {
        binding.btnSubmitReport.isEnabled = true
        binding.btnSubmitReport.text      = "Submit Report"
    }

    // ── Image helpers ─────────────────────────────────────────────────────────

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            openImagePicker()
        else
            permissionLauncher.launch(permission)
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
