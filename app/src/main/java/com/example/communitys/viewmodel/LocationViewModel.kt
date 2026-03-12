package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.model.data.ReportModel
import com.example.communitys.model.repository.ReportRepository
import kotlinx.coroutines.launch

class LocationViewModel : ViewModel() {

    private val reportRepo = ReportRepository()
    private val authHelper = SupabaseAuthHelper()

    private val _reports    = MutableLiveData<List<ReportModel>>()
    val reports: LiveData<List<ReportModel>> = _reports

    private val _isLoading  = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error      = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init { loadReports() }

    fun loadReports() {
        val userId = authHelper.getCurrentUserId() ?: run {
            _error.value = "Not logged in"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            reportRepo.getUserReports(userId)
                .onSuccess { _reports.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
