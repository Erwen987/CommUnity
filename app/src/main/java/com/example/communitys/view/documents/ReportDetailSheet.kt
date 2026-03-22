package com.example.communitys.view.documents

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import com.example.communitys.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

class ReportDetailSheet : BottomSheetDialogFragment(), OnMapReadyCallback {

    companion object {
        private const val ARG_PROBLEM     = "problem"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_STATUS      = "status"
        private const val ARG_DATE        = "date"
        private const val ARG_IMAGE_URL   = "image_url"
        private const val ARG_POINTS      = "points"
        private const val ARG_LAT              = "location_lat"
        private const val ARG_LNG              = "location_lng"
        private const val ARG_REJECTION_REASON = "rejection_reason"

        fun newInstance(
            problem: String,
            description: String,
            status: String,
            date: String,
            imageUrl: String?,
            pointsAwarded: Int?,
            locationLat: Double? = null,
            locationLng: Double? = null,
            rejectionReason: String? = null
        ): ReportDetailSheet {
            return ReportDetailSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROBLEM,          problem)
                    putString(ARG_DESCRIPTION,      description)
                    putString(ARG_STATUS,           status)
                    putString(ARG_DATE,             date)
                    putString(ARG_IMAGE_URL,        imageUrl)
                    putString(ARG_REJECTION_REASON, rejectionReason)
                    if (pointsAwarded != null) putInt(ARG_POINTS, pointsAwarded)
                    if (locationLat   != null) putDouble(ARG_LAT, locationLat)
                    if (locationLng   != null) putDouble(ARG_LNG, locationLng)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_report_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val problem     = arguments?.getString(ARG_PROBLEM)     ?: ""
        val description = arguments?.getString(ARG_DESCRIPTION) ?: ""
        val status      = arguments?.getString(ARG_STATUS)      ?: ""
        val date        = arguments?.getString(ARG_DATE)        ?: ""
        val imageUrl    = arguments?.getString(ARG_IMAGE_URL)
        val points          = if (arguments?.containsKey(ARG_POINTS) == true) arguments?.getInt(ARG_POINTS) else null
        val lat             = if (arguments?.containsKey(ARG_LAT) == true) arguments?.getDouble(ARG_LAT) else null
        val lng             = if (arguments?.containsKey(ARG_LNG) == true) arguments?.getDouble(ARG_LNG) else null
        val rejectionReason = arguments?.getString(ARG_REJECTION_REASON)

        val tvStatus          = view.findViewById<TextView>(R.id.tvRptDetailStatus)
        val tvProblem         = view.findViewById<TextView>(R.id.tvRptDetailProblem)
        val tvDate            = view.findViewById<TextView>(R.id.tvRptDetailDate)
        val tvDescription     = view.findViewById<TextView>(R.id.tvRptDetailDescription)
        val layoutPoints      = view.findViewById<LinearLayout>(R.id.layoutRptPoints)
        val tvPoints          = view.findViewById<TextView>(R.id.tvRptDetailPoints)
        val layoutImage       = view.findViewById<LinearLayout>(R.id.layoutRptImage)
        val ivImage           = view.findViewById<ImageView>(R.id.ivRptDetailImage)
        val layoutMap         = view.findViewById<LinearLayout>(R.id.layoutRptMap)
        val layoutRejection   = view.findViewById<LinearLayout>(R.id.layoutRejectionReason)
        val tvRejectionReason = view.findViewById<TextView>(R.id.tvRejectionReason)

        // Status badge
        val (label, color) = statusInfo(status)
        tvStatus.text = label
        tvStatus.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f
            setColor(color)
        }

        tvProblem.text     = problem.ifBlank { "Report" }
        tvDate.text        = formatDate(date)
        tvDescription.text = description.ifBlank { "—" }

        // Points
        if (points != null && points > 0) {
            layoutPoints.visibility = View.VISIBLE
            tvPoints.text = "+$points pts"
        }

        // Image
        if (!imageUrl.isNullOrBlank()) {
            layoutImage.visibility = View.VISIBLE
            ivImage.load(imageUrl) { crossfade(300) }
        }

        // Rejection reason
        if (status == "rejected" && !rejectionReason.isNullOrBlank()) {
            layoutRejection.visibility = View.VISIBLE
            tvRejectionReason.text = rejectionReason
        }

        // Map — show only if location was saved
        if (lat != null && lng != null) {
            layoutMap.visibility = View.VISIBLE
            val mapFrag = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.rptMapContainer, mapFrag)
                .commit()
            mapFrag.getMapAsync(this)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        val lat = if (arguments?.containsKey(ARG_LAT) == true) arguments?.getDouble(ARG_LAT) else null
        val lng = if (arguments?.containsKey(ARG_LNG) == true) arguments?.getDouble(ARG_LNG) else null
        if (lat == null || lng == null) return

        val position = LatLng(lat, lng)
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false

        map.addMarker(
            MarkerOptions()
                .position(position)
                .title("Reported Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))
    }

    private fun statusInfo(status: String): Pair<String, Int> = when (status) {
        "pending"     -> "Pending"     to Color.parseColor("#F59E0B")
        "in_progress" -> "In Progress" to Color.parseColor("#3B82F6")
        "resolved"    -> "Resolved"    to Color.parseColor("#16A34A")
        "rejected"    -> "Rejected"    to Color.parseColor("#EF4444")
        else          -> status.replace('_', ' ').replaceFirstChar { it.uppercase() } to Color.GRAY
    }

    private fun formatDate(raw: String): String {
        return try {
            val inFmt  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outFmt = SimpleDateFormat("MMMM d, yyyy  •  h:mm a", Locale.getDefault())
            val date   = inFmt.parse(raw.take(19)) ?: return raw
            outFmt.format(date)
        } catch (e: Exception) { raw.take(10) }
    }
}
