package com.example.communitys.view.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.communitys.R
import com.example.communitys.model.data.AnnouncementModel

class AnnouncementAdapter(
    private var items: List<AnnouncementModel> = emptyList()
) : RecyclerView.Adapter<AnnouncementAdapter.VH>() {

    // Pairs of [start color, end color] for diagonal gradients
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

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val vBg: View          = view.findViewById(R.id.vBgColor)
        val ivImage: ImageView = view.findViewById(R.id.ivAnnouncementImage)
        val tvTitle: TextView  = view.findViewById(R.id.tvAnnouncementTitle)
        val tvBody: TextView   = view.findViewById(R.id.tvAnnouncementBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Apply diagonal gradient background
        val colors = gradients[position % gradients.size]
        val gradient = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        holder.vBg.background = gradient

        // Image
        if (!item.imageUrl.isNullOrBlank()) {
            holder.ivImage.visibility = View.VISIBLE
            holder.ivImage.load(item.imageUrl) {
                crossfade(300)
                error(android.R.color.darker_gray)
            }
        } else {
            holder.ivImage.visibility = View.GONE
        }

        holder.tvTitle.text = item.title
        holder.tvBody.text  = item.body
        holder.tvBody.visibility = if (item.body.isBlank()) View.GONE else View.VISIBLE
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<AnnouncementModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
