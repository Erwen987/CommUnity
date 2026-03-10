package com.example.communitys.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {

    private val _reports = MutableLiveData<List<LocationReport>>()
    val reports: LiveData<List<LocationReport>> = _reports

    private val _selectedFilter = MutableLiveData<ReportFilter>()
    val selectedFilter: LiveData<ReportFilter> = _selectedFilter

    init {
        _selectedFilter.value = ReportFilter.ALL
        loadReports()
    }

    private fun loadReports() {
        // Mock data - replace with actual repository call
        val mockReports = listOf(
            LocationReport(
                id = "1",
                title = "Broken Street Light",
                location = "Main Street, Zone 1",
                status = ReportStatus.IN_PROGRESS,
                date = "Feb 25, 2026"
            ),
            LocationReport(
                id = "2",
                title = "Pothole on Road",
                location = "Barangay Road, Zone 2",
                status = ReportStatus.RESOLVED,
                date = "Feb 20, 2026"
            ),
            LocationReport(
                id = "3",
                title = "Garbage Collection Issue",
                location = "Zone 3",
                status = ReportStatus.PENDING,
                date = "Feb 27, 2026"
            )
        )
        _reports.value = mockReports
    }

    fun filterReports(filter: ReportFilter) {
        _selectedFilter.value = filter
        // Apply filter logic here
    }

    data class LocationReport(
        val id: String,
        val title: String,
        val location: String,
        val status: ReportStatus,
        val date: String
    )

    enum class ReportStatus {
        PENDING,
        IN_PROGRESS,
        RESOLVED
    }

    enum class ReportFilter {
        ALL,
        PENDING,
        IN_PROGRESS,
        RESOLVED
    }
}
