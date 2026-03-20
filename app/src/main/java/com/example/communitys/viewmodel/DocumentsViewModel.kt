package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.model.repository.RequestRepository
import kotlinx.coroutines.launch

class DocumentsViewModel : ViewModel() {

    data class DocumentItem(
        val id: String,
        val title: String,
        val reference: String,
        val status: String,
        val date: String,
        val rejectionReason: String? = null
    )

    private val requestRepo = RequestRepository()
    private val authHelper  = SupabaseAuthHelper()

    private val _items     = MutableLiveData<List<DocumentItem>>()
    val items: LiveData<List<DocumentItem>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allRequests = listOf<DocumentItem>()

    // "requests" | "history"
    private var currentTab    = "requests"
    private var currentFilter = "all"

    init { loadAll() }

    fun loadAll() {
        val userId = authHelper.getCurrentUserId() ?: run {
            _error.value = "Not logged in"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true

            requestRepo.getUserRequests(userId).onSuccess { list ->
                allRequests = list.map { r ->
                    DocumentItem(
                        id              = r.id,
                        title           = r.documentType,
                        reference       = r.referenceNumber.ifEmpty { "—" },
                        status          = r.status,
                        date            = r.createdAt,
                        rejectionReason = r.rejectionReason
                    )
                }
            }.onFailure { _error.value = it.message }

            _isLoading.value = false
            applyFilter()
        }
    }

    fun setTab(tab: String) {
        currentTab    = tab
        currentFilter = "all"
        applyFilter()
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        applyFilter()
    }

    private val activeStatuses  = setOf("pending", "ready_for_pickup")
    private val historyStatuses = setOf("claimed", "rejected")

    private fun applyFilter() {
        _items.value = when (currentTab) {
            "history" -> allRequests.filter { it.status in historyStatuses }
            else -> {
                val active = allRequests.filter { it.status in activeStatuses }
                if (currentFilter == "all") active
                else active.filter { it.status == currentFilter }
            }
        }
    }
}
