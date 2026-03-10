package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.model.repository.ReportRepository
import kotlinx.coroutines.launch

class ReportIssueViewModel : ViewModel() {

    private val reportRepository = ReportRepository()

    private val _reportState = MutableLiveData<ReportState>()
    val reportState: LiveData<ReportState> = _reportState

    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    fun submitReport(
        category: String,
        description: String,
        location: String,
        imageUri: String?
    ) {
        // Validate inputs
        val errors = ValidationErrors()

        if (category.isEmpty()) {
            errors.categoryError = "Please select a category"
        }

        if (description.trim().isEmpty()) {
            errors.descriptionError = "Description is required"
        } else if (description.trim().length < 10) {
            errors.descriptionError = "Description must be at least 10 characters"
        }

        if (location.trim().isEmpty()) {
            errors.locationError = "Location is required"
        }

        if (errors.hasErrors()) {
            _validationErrors.value = errors
            return
        }

        // Clear errors and start loading
        _validationErrors.value = ValidationErrors()
        _reportState.value = ReportState.Loading

        viewModelScope.launch {
            val result = reportRepository.submitReport(
                category = category,
                description = description.trim(),
                location = location.trim(),
                imageUrl = imageUri
            )

            result.onSuccess {
                _reportState.value = ReportState.Success("Report submitted successfully!")
            }.onFailure { exception ->
                _reportState.value = ReportState.Error(exception.message ?: "Failed to submit report")
            }
        }
    }

    sealed class ReportState {
        object Loading : ReportState()
        data class Success(val message: String) : ReportState()
        data class Error(val message: String) : ReportState()
    }

    data class ValidationErrors(
        var categoryError: String? = null,
        var descriptionError: String? = null,
        var locationError: String? = null
    ) {
        fun hasErrors(): Boolean {
            return categoryError != null || descriptionError != null || locationError != null
        }
    }
}
