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
        "pending"          -> "Pending"
        "ready_for_pickup" -> "Ready for Pickup"
        "claimed"          -> "Claimed"
        "rejected"         -> "Rejected"
        else               -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun statusColor(status: String): Int = when (status) {
        "pending"          -> Color.parseColor("#F59E0B")
        "ready_for_pickup" -> Color.parseColor("#8B5CF6")
        "claimed"          -> Color.parseColor("#16A34A")
        "rejected"         -> Color.parseColor("#EF4444")
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
