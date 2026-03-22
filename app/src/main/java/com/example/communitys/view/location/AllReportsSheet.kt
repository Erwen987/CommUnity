package com.example.communitys.view.location

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.R
import com.example.communitys.model.data.ReportModel
import com.example.communitys.view.documents.ReportDetailSheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class AllReportsSheet : BottomSheetDialogFragment() {

    companion object {
        private const val PAGE_SIZE = 5
        private var allReports: List<ReportModel> = emptyList()

        fun newInstance(reports: List<ReportModel>): AllReportsSheet {
            allReports = reports
            return AllReportsSheet().apply {
                arguments = Bundle().apply { putInt("count", reports.size) }
            }
        }
    }

    private var currentFilter = "all"
    private var currentPage   = 0
    private var filteredList: List<ReportModel> = emptyList()
    private lateinit var adapter: LocationReportsAdapter

    // ── Expand to ~88% of screen ──────────────────────────────────────────────

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (sheet != null) {
                val height = (resources.displayMetrics.heightPixels * 0.88).toInt()
                sheet.layoutParams.height = height
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight    = height
                behavior.state         = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_all_reports, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvCount      = view.findViewById<TextView>(R.id.tvAllReportsCount)
        val tvPageInd    = view.findViewById<TextView>(R.id.tvPageIndicator)
        val tvEmpty      = view.findViewById<TextView>(R.id.tvAllReportsEmpty)
        val recycler     = view.findViewById<RecyclerView>(R.id.recyclerAllReports)
        val layoutPager  = view.findViewById<View>(R.id.layoutPagination)
        val tvPageNum    = view.findViewById<TextView>(R.id.tvPageNum)
        val btnPrev      = view.findViewById<MaterialButton>(R.id.btnPrev)
        val btnNext      = view.findViewById<MaterialButton>(R.id.btnNext)

        val chipAll        = view.findViewById<Chip>(R.id.chipFilterAll)
        val chipPending    = view.findViewById<Chip>(R.id.chipFilterPending)
        val chipInProgress = view.findViewById<Chip>(R.id.chipFilterInProgress)
        val chipResolved   = view.findViewById<Chip>(R.id.chipFilterResolved)
        val chipHistory    = view.findViewById<Chip>(R.id.chipFilterHistory)
        val chips = listOf(chipAll, chipPending, chipInProgress, chipResolved, chipHistory)

        tvCount.text = "${allReports.size} total report${if (allReports.size != 1) "s" else ""}"

        // ── Adapter ──────────────────────────────────────────────────────────

        adapter = LocationReportsAdapter(onViewDetails = { item ->
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
        })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // ── Chip styling ──────────────────────────────────────────────────────

        fun styleChips(activeId: Int) {
            chips.forEach { chip ->
                if (chip.id == activeId) {
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#2C5F7F"))
                    chip.setTextColor(Color.WHITE)
                } else {
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                    chip.setTextColor(Color.parseColor("#6b7280"))
                }
            }
        }
        styleChips(R.id.chipFilterAll)

        // ── Render page ───────────────────────────────────────────────────────

        fun render() {
            val total      = filteredList.size
            val totalPages = if (total == 0) 1 else (total + PAGE_SIZE - 1) / PAGE_SIZE
            currentPage    = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))

            val start    = currentPage * PAGE_SIZE
            val end      = minOf(start + PAGE_SIZE, total)
            val pageList = if (total == 0) emptyList() else filteredList.subList(start, end)

            adapter.updateList(pageList)

            val isEmpty = total == 0
            tvEmpty.visibility  = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE    else View.VISIBLE

            // Page indicator in header
            if (!isEmpty) {
                tvPageInd.text = "${start + 1}–$end of $total"
            } else {
                tvPageInd.text = ""
            }

            // Pagination bar
            if (totalPages <= 1) {
                layoutPager.visibility = View.GONE
            } else {
                layoutPager.visibility = View.VISIBLE
                tvPageNum.text = "Page ${currentPage + 1} of $totalPages"
                btnPrev.isEnabled = currentPage > 0
                btnPrev.backgroundTintList = ColorStateList.valueOf(
                    if (currentPage > 0) Color.parseColor("#e5e7eb") else Color.parseColor("#f9fafb")
                )
                btnPrev.setTextColor(
                    if (currentPage > 0) Color.parseColor("#374151") else Color.parseColor("#d1d5db")
                )
                btnNext.isEnabled = currentPage < totalPages - 1
                btnNext.backgroundTintList = ColorStateList.valueOf(
                    if (currentPage < totalPages - 1) Color.parseColor("#2C5F7F") else Color.parseColor("#d1d5db")
                )
            }
        }

        // ── Apply filter ──────────────────────────────────────────────────────

        fun applyFilter(filter: String) {
            currentFilter = filter
            currentPage   = 0
            filteredList  = when (filter) {
                "pending"     -> allReports.filter { it.status == "pending" }
                "in_progress" -> allReports.filter { it.status == "in_progress" }
                "resolved"    -> allReports.filter { it.status == "resolved" }
                "history"     -> allReports.filter { it.status == "rejected" }
                else          -> allReports
            }
            render()
        }

        // ── Chip clicks ───────────────────────────────────────────────────────

        chipAll.setOnClickListener        { styleChips(R.id.chipFilterAll);        applyFilter("all") }
        chipPending.setOnClickListener    { styleChips(R.id.chipFilterPending);    applyFilter("pending") }
        chipInProgress.setOnClickListener { styleChips(R.id.chipFilterInProgress); applyFilter("in_progress") }
        chipResolved.setOnClickListener   { styleChips(R.id.chipFilterResolved);   applyFilter("resolved") }
        chipHistory.setOnClickListener    { styleChips(R.id.chipFilterHistory);    applyFilter("history") }

        // ── Pagination clicks ─────────────────────────────────────────────────

        btnPrev.setOnClickListener { currentPage--; render() }
        btnNext.setOnClickListener { currentPage++; render() }

        // ── Initial render ────────────────────────────────────────────────────

        applyFilter("all")
    }
}
