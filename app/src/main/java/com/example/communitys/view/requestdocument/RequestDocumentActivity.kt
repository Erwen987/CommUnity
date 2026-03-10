package com.example.communitys.view.requestdocument

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.communitys.databinding.ActivityRequestDocumentBinding

class RequestDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestDocumentBinding

    // All requestable barangay documents
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

    private var selectedPayment: String = "gcash" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDocumentDropdown()
        setupPaymentSelection()
        setupClickListeners()
    }

    // ── Document dropdown ─────────────────────────────────────────────────────

    private fun setupDocumentDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            documentTypes
        )
        binding.actvDocument.setAdapter(adapter)

        binding.actvDocument.setOnItemClickListener { _, _, position, _ ->
            val selected = documentTypes[position]

            // Clear dropdown error
            binding.tilDocument.error = null
            binding.tilDocument.isErrorEnabled = false

            // Show/hide "Others" text field
            if (selected == "Others") {
                binding.tilOtherDocument.visibility = View.VISIBLE
                binding.etOtherDocument.requestFocus()
            } else {
                binding.tilOtherDocument.visibility = View.GONE
                binding.etOtherDocument.text?.clear()
                binding.tilOtherDocument.error = null
            }
        }
    }

    // ── Payment selection ─────────────────────────────────────────────────────

    private fun setupPaymentSelection() {
        // Default: GCash selected
        setPaymentSelected("gcash")

        binding.cvGCash.setOnClickListener {
            setPaymentSelected("gcash")
        }

        binding.cvPayOnSite.setOnClickListener {
            setPaymentSelected("payonsite")
        }
    }

    private fun setPaymentSelected(method: String) {
        selectedPayment = method

        if (method == "gcash") {
            binding.cvGCash.cardElevation = 8f
            binding.cvPayOnSite.cardElevation = 2f
            // Show proof of payment upload
            binding.tvProofOfPayment.visibility = View.VISIBLE
            binding.cvUploadProof.visibility = View.VISIBLE
        } else {
            binding.cvGCash.cardElevation = 2f
            binding.cvPayOnSite.cardElevation = 8f
            // Hide proof of payment upload
            binding.tvProofOfPayment.visibility = View.GONE
            binding.cvUploadProof.visibility = View.GONE
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSubmitRequest.setOnClickListener {
            if (validateForm()) {
                submitRequest()
            }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate document type
        val selectedDoc = binding.actvDocument.text.toString().trim()
        if (selectedDoc.isEmpty() || selectedDoc !in documentTypes) {
            binding.tilDocument.error = "Please select a document type"
            binding.tilDocument.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilDocument.error = null
            binding.tilDocument.isErrorEnabled = false
        }

        // Validate "Others" field if visible
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

        // Validate purpose
        val purpose = binding.etPurpose.text.toString().trim()
        if (purpose.isEmpty()) {
            binding.tilPurpose.error = "Please enter the purpose"
            binding.tilPurpose.isErrorEnabled = true
            isValid = false
        } else {
            binding.tilPurpose.error = null
            binding.tilPurpose.isErrorEnabled = false
        }

        return isValid
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private fun submitRequest() {
        val selectedDoc = binding.actvDocument.text.toString().trim()
        val finalDocumentType = if (selectedDoc == "Others") {
            binding.etOtherDocument.text.toString().trim()
        } else {
            selectedDoc
        }
        val purpose = binding.etPurpose.text.toString().trim()

        // TODO: Save to Supabase document_requests table
        Toast.makeText(
            this,
            "Request for \"$finalDocumentType\" submitted!\nWe'll notify you when it's ready.",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}