package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.model.data.RequestModel
import com.example.communitys.model.repository.RequestRepository
import kotlinx.coroutines.launch

class DocumentsViewModel : ViewModel() {

    private val repository = RequestRepository()
    private val authHelper = SupabaseAuthHelper()

    private val _requests = MutableLiveData<List<RequestModel>>()
    val requests: LiveData<List<RequestModel>> = _requests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allRequests = listOf<RequestModel>()
    private var currentTab = "active"   // "active" or "history"
    private var currentFilter = "all"   // "all" | "reviewing" | "processing" | "ready_for_pickup" | "released"

    init {
        loadRequests()
    }

    fun loadRequests() {
        val userId = authHelper.getCurrentUserId() ?: run {
            _error.value = "Not logged in"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            repository.getUserRequests(userId)
                .onSuccess { list ->
                    allRequests = list
                    applyFilter()
                }
                .onFailure { e ->
                    _error.value = e.message
                }
            _isLoading.value = false
        }
    }

    fun setTab(tab: String) {
        currentTab = tab
        // Reset chip filter when switching tabs
        currentFilter = "all"
        applyFilter()
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        applyFilter()
    }

    private fun applyFilter() {
        var filtered = allRequests

        if (currentTab == "history") {
            // History tab: only released or rejected
            filtered = filtered.filter { it.status == "released" || it.status == "rejected" }
        } else {
            // My Request tab: active items, apply chip filter
            if (currentFilter != "all") {
                filtered = filtered.filter { it.status == currentFilter }
            }
        }

        _requests.value = filtered
    }
}
