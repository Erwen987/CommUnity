package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DocumentsViewModel : ViewModel() {

    private val _documents = MutableLiveData<List<Document>>()
    val documents: LiveData<List<Document>> = _documents

    private val _filterType = MutableLiveData<FilterType>()
    val filterType: LiveData<FilterType> = _filterType

    init {
        _filterType.value = FilterType.ALL
        loadDocuments()
    }

    private fun loadDocuments() {
        // Mock data - replace with actual repository call
        val mockDocuments = listOf(
            Document(
                id = "1",
                title = "Barangay Clearance",
                status = DocumentStatus.PROCESSING,
                requestDate = "Feb 20, 2026",
                type = "Clearance"
            ),
            Document(
                id = "2",
                title = "Certificate of Residency",
                status = DocumentStatus.RELEASED,
                requestDate = "Feb 15, 2026",
                type = "Certificate"
            ),
            Document(
                id = "3",
                title = "Business Permit",
                status = DocumentStatus.PROCESSING,
                requestDate = "Feb 18, 2026",
                type = "Permit"
            )
        )
        _documents.value = mockDocuments
    }

    fun filterDocuments(filterType: FilterType) {
        _filterType.value = filterType
        // Apply filter logic here
    }

    data class Document(
        val id: String,
        val title: String,
        val status: DocumentStatus,
        val requestDate: String,
        val type: String
    )

    enum class DocumentStatus {
        PROCESSING,
        RELEASED,
        REJECTED
    }

    enum class FilterType {
        ALL,
        PROCESSING,
        RELEASED
    }
}
