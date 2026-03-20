package com.example.communitys.view.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.example.communitys.R
import com.example.communitys.model.data.AnnouncementModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AnnouncementDetailSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TITLE     = "title"
        private const val ARG_BODY      = "body"
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_CREATED   = "created_at"
        private const val ARG_EXPIRES   = "expires_at"
        private const val ARG_COLOR_IDX = "color_idx"

        fun newInstance(item: AnnouncementModel, colorIndex: Int): AnnouncementDetailSheet {
            return AnnouncementDetailSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE,     item.title)
                    putString(ARG_BODY,      item.body)
                    putString(ARG_IMAGE_URL, item.imageUrl)
                    putString(ARG_CREATED,   item.createdAt)
                    putString(ARG_EXPIRES,   item.expiresAt)
                    putInt(ARG_COLOR_IDX,    colorIndex)
                }
            }
        }
    }

    private val gradients = listOf(
        intArrayOf(Color.parseColor("#1E3A5F"), Color.parseColor("#0a2240")),
        intArrayOf(Color.parseColor("#0f766e"), Color.parseColor("#064e49")),
        intArrayOf(Color.parseColor("#6d28d9"), Color.parseColor("#4c1d95")),
        intArrayOf(Color.parseColor("#b45309"), Color.parseColor("#7c2d12")),
        intArrayOf(Color.parseColor("#0369a1"), Color.parseColor("#0c4a6e")),
        intArrayOf(Color.parseColor("#be185d"), Color.parseColor("#831843")),
        intArrayOf(Color.parseColor("#1d4ed8"), Color.parseColor("#1e3a8a")),
        intArrayOf(Color.parseColor("#15803d"), Color.parseColor("#14532d")),
        intArrayOf(Color.parseColor("#b91c1c"), Color.parseColor("#7f1d1d")),
        intArrayOf(Color.parseColor("#0e7490"), Color.parseColor("#164e63")),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_announcement, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title     = arguments?.getString(ARG_TITLE)     ?: ""
        val body      = arguments?.getString(ARG_BODY)      ?: ""
        val imageUrl  = arguments?.getString(ARG_IMAGE_URL)
        val createdAt = arguments?.getString(ARG_CREATED)   ?: ""
        val expiresAt = arguments?.getString(ARG_EXPIRES)   ?: ""
        val colorIdx  = arguments?.getInt(ARG_COLOR_IDX, 0) ?: 0

        val ivImage      = view.findViewById<ImageView>(R.id.ivDetailImage)
        val vColorBar    = view.findViewById<View>(R.id.vDetailColorBar)
        val tvTitle      = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDate       = view.findViewById<TextView>(R.id.tvDetailDate)
        val tvExpiry     = view.findViewById<TextView>(R.id.tvDetailExpiry)
        val tvBody       = view.findViewById<TextView>(R.id.tvDetailBody)
        val tvNoBody     = view.findViewById<TextView>(R.id.tvDetailNoBody)

        // Image or color bar
        if (!imageUrl.isNullOrBlank()) {
            ivImage.visibility = View.VISIBLE
            ivImage.load(imageUrl) { crossfade(300) }
        } else {
            vColorBar.visibility = View.VISIBLE
            val colors = gradients[colorIdx % gradients.size]
            val gd = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
            vColorBar.background = gd
        }

        tvTitle.text = title

        // Format date
        tvDate.text = formatDate(createdAt)

        // Days remaining
        tvExpiry.text = daysLeft(expiresAt)

        // Body
        if (body.isNotBlank()) {
            tvBody.visibility   = View.VISIBLE
            tvNoBody.visibility = View.GONE
            tvBody.text         = body
        } else {
            tvBody.visibility   = View.GONE
            tvNoBody.visibility = View.VISIBLE
        }
    }

    private fun formatDate(raw: String): String {
        return try {
            val inFmt  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date   = inFmt.parse(raw.take(19)) ?: Date()
            "Posted ${outFmt.format(date)}"
        } catch (e: Exception) { "" }
    }

    private fun daysLeft(raw: String): String {
        return try {
            val fmt  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val exp  = fmt.parse(raw.take(19)) ?: return ""
            val diff = exp.time - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days > 0) "${days}d left" else "Expires today"
        } catch (e: Exception) { "" }
    }
}
