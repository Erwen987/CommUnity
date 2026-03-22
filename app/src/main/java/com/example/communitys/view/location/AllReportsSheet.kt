package com.example.communitys.view.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.R
import com.example.communitys.model.data.ReportModel
import com.example.communitys.view.documents.ReportDetailSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AllReportsSheet : BottomSheetDialogFragment() {

    companion object {
        private var allReports: List<ReportModel> = emptyList()

        fun newInstance(reports: List<ReportModel>): AllReportsSheet {
            allReports = reports
            return AllReportsSheet().apply {
                arguments = Bundle().apply { putInt("count", reports.size) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_all_reports, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val count = arguments?.getInt("count") ?: 0
        view.findViewById<TextView>(R.id.tvAllReportsCount).text =
            "$count report${if (count != 1) "s" else ""}"

        val adapter = LocationReportsAdapter(
            items = allReports,
            onViewDetails = { item ->
                ReportDetailSheet.newInstance(
                    problem       = item.problem,
                    description   = item.description,
                    status        = item.status,
                    date          = item.createdAt,
                    imageUrl      = item.imageUrl,
                    pointsAwarded = item.pointsAwarded,
                    locationLat   = item.locationLat,
                    locationLng   = item.locationLng
                ).show(parentFragmentManager, "report_detail")
            }
        )

        view.findViewById<RecyclerView>(R.id.recyclerAllReports).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }
}
