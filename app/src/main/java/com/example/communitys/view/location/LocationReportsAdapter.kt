package com.example.communitys.view.location

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.databinding.ItemReportBinding
import com.example.communitys.model.data.ReportModel
import java.text.SimpleDateFormat
import java.util.Locale

class LocationReportsAdapter(
    private var items: List<ReportModel> = emptyList()
) : RecyclerView.Adapter<LocationReportsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReportBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvReportTitle.text       = item.problem.ifEmpty { item.description.take(50) }
            tvReportDescription.text = item.description
            tvReportDate.text        = formatDate(item.createdAt)
            tvReportStatus.text      = formatStatus(item.status)

            when {
                item.pointsAwarded != null -> {
                    tvReportPoints.text = "🌟 +${item.pointsAwarded} pts earned"
                    tvReportPoints.setTextColor(Color.parseColor("#4CAF50"))
                }
                item.status == "rejected" -> {
                    tvReportPoints.text = "— No points"
                    tvReportPoints.setTextColor(Color.parseColor("#BDBDBD"))
                }
                else -> {
                    tvReportPoints.text = "⏳ Pts pending official review"
                    tvReportPoints.setTextColor(Color.parseColor("#9E9E9E"))
                }
            }

            val color = statusColor(item.status)

            val badge = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(color)
            }
            tvReportStatus.background = badge
            viewStripe.setBackgroundColor(color)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<ReportModel>) {
        items = newList
        notifyDataSetChanged()
    }

    private fun formatStatus(status: String): String = when (status) {
        "pending"     -> "Pending"
        "in_progress" -> "In Progress"
        "resolved"    -> "Resolved"
        "rejected"    -> "Rejected"
        else          -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun statusColor(status: String): Int = when (status) {
        "pending"     -> Color.parseColor("#9E9E9E")
        "in_progress" -> Color.parseColor("#FF9800")
        "resolved"    -> Color.parseColor("#4CAF50")
        "rejected"    -> Color.parseColor("#F44336")
        else          -> Color.GRAY
    }

    private fun formatDate(date: String): String {
        return try {
            val input  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val parsed = input.parse(date.take(19)) ?: return date
            output.format(parsed)
        } catch (e: Exception) {
            date.take(10)
        }
    }
}
