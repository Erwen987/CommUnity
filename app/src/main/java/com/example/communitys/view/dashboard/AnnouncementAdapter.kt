package com.example.communitys.view.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.communitys.R
import com.example.communitys.model.data.AnnouncementModel

class AnnouncementAdapter(
    private var items: List<AnnouncementModel> = emptyList()
) : RecyclerView.Adapter<AnnouncementAdapter.VH>() {

    // Cycling background colors for text-only announcements
    private val bgColors = listOf(
        "#1E3A5F", "#0f766e", "#7c3aed", "#b45309", "#0369a1",
        "#be185d", "#065f46", "#1d4ed8", "#9a3412", "#4338ca"
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

        // Background color (cycles through palette)
        holder.vBg.setBackgroundColor(Color.parseColor(bgColors[position % bgColors.size]))

        // Image
        if (!item.imageUrl.isNullOrBlank()) {
            holder.ivImage.visibility = View.VISIBLE
            holder.ivImage.load(item.imageUrl) {
                crossfade(true)
                error(android.R.color.darker_gray)
            }
        } else {
            holder.ivImage.visibility = View.GONE
        }

        holder.tvTitle.text = item.title
        holder.tvBody.text  = item.body.ifBlank { "" }
        holder.tvBody.visibility = if (item.body.isBlank()) View.GONE else View.VISIBLE
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<AnnouncementModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
