package com.example.communitys.view.requestdocument

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.communitys.R
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.SupabaseStorageHelper
import com.example.communitys.databinding.ActivityRequestDocumentBinding
import com.example.communitys.model.repository.RequestRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class RequestDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestDocumentBinding
    private val repository     = RequestRepository()
    private val authHelper     = SupabaseAuthHelper()
    private val storageHelper  = SupabaseStorageHelper()

    private val documentTypes = listOf(
        "Barangay Clearance",
        "Certificate of Residency",
        "Certificate of Indigency",
        "Barangay Business Permit",
        "Certificate of Good Moral Character",
        "Certificate of Late Registration",
        "Barangay ID",
        "Barangay Certification",
        "Others"
    )

    private val documentPrices = mapOf(
        "Barangay Clearance" to 50.0,
        "Certificate of Residency" to 30.0,
        "Certificate of Indigency" to 0.0,
        "Barangay Business Permit" to 500.0,
        "Certificate of Good Moral Character" to 50.0,
        "Certificate of Late Registration" to 100.0,
        "Barangay ID" to 100.0,
        "Barangay Certification" to 50.0,
        "Others" to 0.0
    )

    private var selectedPayment: String = "gcash"
    private var selectedProofUri: Uri?  = null
    private var uploadedProofUrl: String? = null
    private var currentQuantity: Int = 1

    // ── Image picker ──────────────────────────────────────────────────────────

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedProofUri = it
            binding.ivProofPreview.setImageURI(it)
            binding.ivProofPreview.visibility = View.VISIBLE
            binding.ivUploadIcon.visibility   = View.GONE
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openImagePicker()
        else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDocumentDropdown()
        setupPaymentSelection()
        setupQuantityControls()
        setupClickListeners()
    }

    // ── Document dropdown ─────────────────────────────────────────────────────

    private fun setupDocumentDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, documentTypes)
        binding.actvDocument.setAdapter(adapter)

        binding.actvDocument.setOnItemClickListener { _, _, position, _ ->
            val selected = documentTypes[position]
            binding.tilDocument.error = null
            binding.tilDocument.isErrorEnabled = false

            // Show price
            val price = documentPrices[selected] ?: 0.0
            if (price > 0) {
                binding.tvDocumentPrice.visibility = View.VISIBLE
                binding.tvDocumentPrice.text = "Price: ₱${String.format("%.2f", price)}"
            } else {
                binding.tvDocumentPrice.visibility = View.VISIBLE
                binding.tvDocumentPrice.text = "Price: FREE"
            }
            
            // Update total price
            updateTotalPrice()

            if (selected == "Others") {
                binding.tvOtherDocumentLabel.visibility = View.VISIBLE
                binding.tilOtherDocument.visibility = View.VISIBLE
                binding.etOtherDocument.requestFocus()
            } else {
                binding.tvOtherDocumentLabel.visibility = View.GONE
                binding.tilOtherDocument.visibility = View.GONE
                binding.etOtherDocument.text?.clear()
                binding.tilOtherDocument.error = null
            }
        }
    }

    // ── Payment selection ─────────────────────────────────────────────────────

    private fun setupQuantityControls() {
        // Set initial value
        binding.etQuantity.setText("1")
        
        binding.btnDecreaseQty.setOnClickListener {
            val current = binding.etQuantity.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                currentQuantity = current - 1
                binding.etQuantity.setText(currentQuantity.toString())
                binding.tilQuantity.error = null
                binding.tilQuantity.isErrorEnabled = false
                updateTotalPrice()
            }
        }
        
        binding.btnIncreaseQty.setOnClickListener {
            val current = binding.etQuantity.text.toString().toIntOrNull() ?: 1
            if (current < 10) {
                currentQuantity = current + 1
                binding.etQuantity.setText(currentQuantity.toString())
                binding.tilQuantity.error = null
                binding.tilQuantity.isErrorEnabled = false
                updateTotalPrice()
            } else {
                Toast.makeText(this, "Maximum quantity is 10", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle manual text input
        binding.etQuantity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    val qty = text.toIntOrNull()
                    if (qty != null && qty in 1..10) {
                        currentQuantity = qty
                        binding.tilQuantity.error = null
                        binding.tilQuantity.isErrorEnabled = false
                        updateTotalPrice()
                    }
                }
            }
        })
        
        binding.etQuantity.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.etQuantity.text.toString()
                if (text.isEmpty()) {
                    binding.etQuantity.setText("1")
                    currentQuantity = 1
                } else {
                    val qty = text.toIntOrNull() ?: 1
                    currentQuantity = qty.coerceIn(1, 10)
                    binding.etQuantity.setText(currentQuantity.toString())
                }
                updateTotalPrice()
            }
        }
    }
    
    private fun updateTotalPrice() {
        val selectedDoc = binding.actvDocument.text.toString().trim()
        if (selectedDoc.isNotEmpty() && selectedDoc in documentTypes) {
            val price = documentPrices[selectedDoc] ?: 0.0
            val total = price * currentQuantity
            
            if (total > 0) {
                binding.tvTotalPrice.visibility = View.VISIBLE
                binding.tvTotalPrice.text = "Total: ₱${String.format("%.2f", total)}"
            } else {
                binding.tvTotalPrice.visibility = View.VISIBLE
                binding.tvTotalPrice.text = "Total: FREE"
            }
        }
    }

    private fun setupPaymentSelection() {
        setPaymentSelected("gcash")
        binding.cvGCash.setOnClickListener    { setPaymentSelected("gcash") }
        binding.cvPayOnSite.setOnClickListener { setPaymentSelected("pay_on_site") }
        binding.btnViewQR.setOnClickListener  { showGCashQRDialog() }
    }

    private fun setPaymentSelected(method: String) {
        selectedPayment = method
        if (method == "gcash") {
            binding.cvGCash.cardElevation    = 8f
            binding.cvPayOnSite.cardElevation = 2f
            binding.btnViewQR.visibility         = View.VISIBLE
            binding.tvProofOfPayment.visibility  = View.VISIBLE
            binding.cvUploadProof.visibility     = View.VISIBLE
        } else {
            binding.cvGCash.cardElevation    = 2f
            binding.cvPayOnSite.cardElevation = 8f
            binding.btnViewQR.visibility         = View.GONE
            binding.tvProofOfPayment.visibility  = View.GONE
            binding.cvUploadProof.visibility     = View.GONE
            // Clear any selected proof when switching away from GCash
            selectedProofUri  = null
            uploadedProofUrl  = null
            binding.ivProofPreview.visibility = View.GONE
            binding.ivUploadIcon.visibility   = View.VISIBLE
        }
    }

    private fun showGCashQRDialog() {
        val sizePx = (260 * resources.displayMetrics.density).toInt()
        val qrView = ImageView(this).apply {
            setImageResource(R.drawable.ic_gcash_qr)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(48, 24, 48, 0)
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                sizePx
            )
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Scan to Pay via GCash / InstaPay")
            .setMessage("Open your GCash app or any InstaPay-supported bank app and scan the QR code.\n\nAfter paying, upload your receipt below as proof of payment.")
            .setView(qrView)
            .setPositiveButton("Got it, I'll pay now") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener         { finish() }
        binding.cvUploadProof.setOnClickListener   { checkPermissionAndPickImage() }
        binding.btnSubmitRequest.setOnClickListener {
            if (validateForm()) submitRequest()
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validateForm(): Boolean {
        var isValid = true

        val selectedDoc = binding.actvDocument.text.toString().trim()
        if (selectedDoc.isEmpty() || selectedDoc !in documentTypes) {
            binding.tilDocument.error = "Please select a document type"
            binding.tilDocument.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilDocument.error = null
            binding.tilDocument.isErrorEnabled = false
        }

        if (binding.tilOtherDocument.visibility == View.VISIBLE) {
            val otherText = binding.etOtherDocument.text.toString().trim()
            if (otherText.isEmpty()) {
                binding.tilOtherDocument.error = "Please specify the document"
                binding.tilOtherDocument.isErrorEnabled = true
                isValid = false
            } else {
                binding.tilOtherDocument.error = null
                binding.tilOtherDocument.isErrorEnabled = false
            }
        }

        val purpose = binding.etPurpose.text.toString().trim()
        if (purpose.isEmpty()) {
            binding.tilPurpose.error = "Please enter the purpose"
            binding.tilPurpose.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilPurpose.error = null
            binding.tilPurpose.isErrorEnabled = false
        }
        
        // Validate quantity
        val qtyText = binding.etQuantity.text.toString().trim()
        if (qtyText.isEmpty()) {
            // Set default quantity if empty
            currentQuantity = 1
            binding.etQuantity.setText("1")
        } else {
            val qty = qtyText.toIntOrNull()
            if (qty == null || qty < 1 || qty > 10) {
                binding.tilQuantity.error = "Quantity must be between 1 and 10"
                binding.tilQuantity.isErrorEnabled = true
                isValid = false
            } else {
                binding.tilQuantity.error = null
                binding.tilQuantity.isErrorEnabled = false
                currentQuantity = qty
            }
        }

        // Require proof of payment when GCash is selected
        if (selectedPayment == "gcash" && selectedProofUri == null) {
            Toast.makeText(this, "Please upload your GCash receipt as proof of payment", Toast.LENGTH_LONG).show()
            isValid = false
        }

        return isValid
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private fun submitRequest() {
        val userId = authHelper.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Not logged in. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDoc = binding.actvDocument.text.toString().trim()
        val finalDocumentType = if (selectedDoc == "Others") {
            binding.etOtherDocument.text.toString().trim()
        } else {
            selectedDoc
        }
        val purpose = binding.etPurpose.text.toString().trim()

        binding.btnSubmitRequest.isEnabled = false
        binding.btnSubmitRequest.text      = "Submitting..."

        lifecycleScope.launch {
            try {
                // Fetch user's barangay for routing
                val barangay = repository.getUserBarangay(userId)

                // Upload proof image if selected
                if (selectedProofUri != null) {
                    val uploadResult = storageHelper.uploadImage(
                        bucketName = "proof-images",
                        fileUri    = selectedProofUri!!,
                        userId     = userId,
                        context    = this@RequestDocumentActivity
                    )
                    if (uploadResult.isSuccess) {
                        uploadedProofUrl = uploadResult.getOrNull()
                    } else {
                        Toast.makeText(
                            this@RequestDocumentActivity,
                            "Failed to upload receipt: ${uploadResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        resetButton()
                        return@launch
                    }
                }

                val result = repository.submitRequest(
                    userId        = userId,
                    documentType  = finalDocumentType,
                    purpose       = purpose,
                    paymentMethod = selectedPayment,
                    proofUrl      = uploadedProofUrl,
                    barangay      = barangay
                    // quantity parameter removed temporarily until database is updated
                )

                result.onSuccess {
                    Toast.makeText(
                        this@RequestDocumentActivity,
                        "Request for \"$finalDocumentType\" submitted!\nYou can track it in My Request.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }.onFailure { e ->
                    Toast.makeText(
                        this@RequestDocumentActivity,
                        "Failed to submit: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetButton()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RequestDocumentActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                resetButton()
            }
        }
    }

    private fun resetButton() {
        binding.btnSubmitRequest.isEnabled = true
        binding.btnSubmitRequest.text      = "Submit Request"
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
}
