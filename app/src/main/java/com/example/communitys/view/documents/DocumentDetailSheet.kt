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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentDetailSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TITLE           = "title"
        private const val ARG_REFERENCE       = "reference"
        private const val ARG_STATUS          = "status"
        private const val ARG_DATE            = "date"
        private const val ARG_PURPOSE         = "purpose"
        private const val ARG_PAYMENT         = "payment"
        private const val ARG_PROOF_URL       = "proof_url"
        private const val ARG_REJECTION       = "rejection"

        fun newInstance(
            title: String,
            reference: String,
            status: String,
            date: String,
            purpose: String,
            paymentMethod: String,
            proofUrl: String?,
            rejectionReason: String?
        ): DocumentDetailSheet {
            return DocumentDetailSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE,     title)
                    putString(ARG_REFERENCE, reference)
                    putString(ARG_STATUS,    status)
                    putString(ARG_DATE,      date)
                    putString(ARG_PURPOSE,   purpose)
                    putString(ARG_PAYMENT,   paymentMethod)
                    putString(ARG_PROOF_URL, proofUrl)
                    putString(ARG_REJECTION, rejectionReason)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_document_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title           = arguments?.getString(ARG_TITLE)     ?: ""
        val reference       = arguments?.getString(ARG_REFERENCE) ?: ""
        val status          = arguments?.getString(ARG_STATUS)    ?: ""
        val date            = arguments?.getString(ARG_DATE)      ?: ""
        val purpose         = arguments?.getString(ARG_PURPOSE)   ?: ""
        val paymentMethod   = arguments?.getString(ARG_PAYMENT)   ?: ""
        val proofUrl        = arguments?.getString(ARG_PROOF_URL)
        val rejectionReason = arguments?.getString(ARG_REJECTION)

        val tvStatus    = view.findViewById<TextView>(R.id.tvDetailStatus)
        val tvTitle     = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvRef       = view.findViewById<TextView>(R.id.tvDetailRef)
        val tvDate      = view.findViewById<TextView>(R.id.tvDetailDate)
        val tvPurpose   = view.findViewById<TextView>(R.id.tvDetailPurpose)
        val tvPayment   = view.findViewById<TextView>(R.id.tvDetailPayment)
        val layoutProof = view.findViewById<LinearLayout>(R.id.layoutProof)
        val ivProof     = view.findViewById<ImageView>(R.id.ivDetailProof)
        val layoutRej   = view.findViewById<LinearLayout>(R.id.layoutRejection)
        val tvRejReason = view.findViewById<TextView>(R.id.tvDetailRejectionReason)

        // Status badge
        val (label, color) = statusInfo(status)
        tvStatus.text = label
        val badge = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f
            setColor(color)
        }
        tvStatus.background = badge

        tvTitle.text   = title
        tvRef.text     = reference
        tvDate.text    = formatDate(date)
        tvPurpose.text = purpose.ifBlank { "—" }
        tvPayment.text = formatPayment(paymentMethod)

        // Proof of payment
        if (!proofUrl.isNullOrBlank()) {
            layoutProof.visibility = View.VISIBLE
            ivProof.load(proofUrl) { crossfade(300) }
        }

        // Rejection reason
        if (status == "rejected" && !rejectionReason.isNullOrBlank()) {
            layoutRej.visibility = View.VISIBLE
            tvRejReason.text     = rejectionReason
        }
    }

    private fun statusInfo(status: String): Pair<String, Int> = when (status) {
        "pending"          -> "Pending"          to Color.parseColor("#F59E0B")
        "ready_for_pickup" -> "Ready for Pickup" to Color.parseColor("#8B5CF6")
        "claimed"          -> "Claimed"          to Color.parseColor("#16A34A")
        "rejected"         -> "Rejected"         to Color.parseColor("#EF4444")
        else               -> status.replaceFirstChar { it.uppercase() } to Color.GRAY
    }

    private fun formatPayment(method: String): String = when (method) {
        "gcash"        -> "GCash"
        "pay_on_site"  -> "Pay on Site"
        else           -> method.replace('_', ' ').replaceFirstChar { it.uppercase() }
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
