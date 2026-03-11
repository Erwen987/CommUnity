package com.example.communitys.view.requestdocument

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.databinding.ActivityRequestDocumentBinding
import com.example.communitys.model.repository.RequestRepository
import kotlinx.coroutines.launch

class RequestDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestDocumentBinding
    private val repository = RequestRepository()
    private val authHelper = SupabaseAuthHelper()

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

    private var selectedPayment: String = "gcash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDocumentDropdown()
        setupPaymentSelection()
        setupClickListeners()
    }

    private fun setupDocumentDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, documentTypes)
        binding.actvDocument.setAdapter(adapter)

        binding.actvDocument.setOnItemClickListener { _, _, position, _ ->
            val selected = documentTypes[position]
            binding.tilDocument.error = null
            binding.tilDocument.isErrorEnabled = false

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

    private fun setupPaymentSelection() {
        setPaymentSelected("gcash")
        binding.cvGCash.setOnClickListener { setPaymentSelected("gcash") }
        binding.cvPayOnSite.setOnClickListener { setPaymentSelected("pay_on_site") }
    }

    private fun setPaymentSelected(method: String) {
        selectedPayment = method
        if (method == "gcash") {
            binding.cvGCash.cardElevation = 8f
            binding.cvPayOnSite.cardElevation = 2f
            binding.tvProofOfPayment.visibility = View.VISIBLE
            binding.cvUploadProof.visibility = View.VISIBLE
        } else {
            binding.cvGCash.cardElevation = 2f
            binding.cvPayOnSite.cardElevation = 8f
            binding.tvProofOfPayment.visibility = View.GONE
            binding.cvUploadProof.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmitRequest.setOnClickListener {
            if (validateForm()) submitRequest()
        }
    }

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

        return isValid
    }

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
        binding.btnSubmitRequest.text = "Submitting..."

        lifecycleScope.launch {
            val result = repository.submitRequest(
                userId = userId,
                documentType = finalDocumentType,
                purpose = purpose,
                paymentMethod = selectedPayment
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
                binding.btnSubmitRequest.isEnabled = true
                binding.btnSubmitRequest.text = "Submit Request"
            }
        }
    }
}
