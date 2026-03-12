package com.example.communitys.view.documents

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.databinding.ItemDocumentRequestBinding
import com.example.communitys.viewmodel.DocumentsViewModel.DocumentItem
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentsAdapter(
    private var items: List<DocumentItem> = emptyList(),
    private val onViewDetails: (DocumentItem) -> Unit = {}
) : RecyclerView.Adapter<DocumentsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDocumentRequestBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDocumentRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvDocumentTitle.text   = item.title
            tvReferenceNumber.text = item.reference
            tvDate.text            = formatDate(item.date)
            tvStatus.text          = formatStatus(item.status)

            val badge = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(statusColor(item.status))
            }
            tvStatus.background = badge

            btnViewDetails.setOnClickListener { onViewDetails(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<DocumentItem>) {
        items = newList
        notifyDataSetChanged()
    }

    private fun formatStatus(status: String): String = when (status) {
        "reviewing"        -> "Reviewing"
        "processing"       -> "Processing"
        "ready_for_pickup" -> "Ready for Pickup"
        "released"         -> "Released"
        "rejected"         -> "Rejected"
        else               -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun statusColor(status: String): Int = when (status) {
        "reviewing"        -> Color.parseColor("#5B9BD5")
        "processing"       -> Color.parseColor("#FF9800")
        "ready_for_pickup" -> Color.parseColor("#9C27B0")
        "released"         -> Color.parseColor("#4CAF50")
        "rejected"         -> Color.parseColor("#F44336")
        else               -> Color.GRAY
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
