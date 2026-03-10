package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communitys.CommUnityApplication
import com.example.communitys.model.data.ReportModel
import com.example.communitys.model.repository.AuthRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel : ViewModel() {

    private val supabase = CommUnityApplication.supabase
    private val authRepository = AuthRepository()

    // ── LiveData ──────────────────────────────────────────────────────────────

    private val _welcomeMessage = MutableLiveData<String>()
    val welcomeMessage: LiveData<String> = _welcomeMessage

    private val _locationDate = MutableLiveData<String>()
    val locationDate: LiveData<String> = _locationDate

    private val _reportsSubmitted = MutableLiveData<Int>(0)
    val reportsSubmitted: LiveData<Int> = _reportsSubmitted

    private val _inProgress = MutableLiveData<Int>(0)
    val inProgress: LiveData<Int> = _inProgress

    private val _resolved = MutableLiveData<Int>(0)
    val resolved: LiveData<Int> = _resolved

    private val _pointsEarned = MutableLiveData<Int>(0)
    val pointsEarned: LiveData<Int> = _pointsEarned

    // ── Load all data ─────────────────────────────────────────────────────────

    fun loadUserData(isReturningUser: Boolean) {
        viewModelScope.launch {
            try {
                val result = authRepository.getCurrentUser()

                result.onSuccess { user ->
                    val greeting = if (isReturningUser) "Welcome back" else "Welcome"
                    _welcomeMessage.value = "$greeting, ${user.firstName}! 👋"
                    _locationDate.value = "${formatBarangay(user.barangay)} • ${getCurrentDate()}"
                    _pointsEarned.value = user.points
                }

                result.onFailure {
                    val greeting = if (isReturningUser) "Welcome back! 👋" else "Welcome! 👋"
                    _welcomeMessage.value = greeting
                    _locationDate.value = getCurrentDate()
                }

            } catch (e: Exception) {
                _welcomeMessage.value = if (isReturningUser) "Welcome back! 👋" else "Welcome! 👋"
                _locationDate.value = getCurrentDate()
            }

            // Load report stats separately
            loadReportStats()
        }
    }

    // ── Load report stats from Supabase ───────────────────────────────────────

    private suspend fun loadReportStats() {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return

            val reports = supabase.from("reports")
                .select {
                    filter { eq("user_id", userId.toString()) }
                }
                .decodeList<ReportModel>()

            // Count by status
            _reportsSubmitted.value = reports.size
            _inProgress.value = reports.count { it.status == "in_progress" }
            _resolved.value = reports.count { it.status == "resolved" }

        } catch (e: Exception) {
            android.util.Log.e("DashboardViewModel", "Failed to load stats: ${e.message}")
        }
    }

    // ── Refresh stats (call this when returning to dashboard) ─────────────────

    fun refreshStats() {
        viewModelScope.launch {
            loadReportStats()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatBarangay(barangay: String): String {
        if (barangay.isBlank()) return "Dagupan City, Pangasinan"
        val trimmed = barangay.trim()
        val prefix = if (trimmed.startsWith("Barangay", ignoreCase = true)) "" else "Barangay "
        return "${prefix}${trimmed}, Dagupan City, Pangasinan"
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    }
}