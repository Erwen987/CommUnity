package com.example.communitys.view.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.communitys.R
import com.example.communitys.databinding.ItemNotificationBinding
import com.example.communitys.model.data.NotificationModel
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val onNotificationClick: (NotificationModel) -> Unit,
    private val onDeleteClick: (NotificationModel) -> Unit
) : ListAdapter<NotificationModel, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationModel) {
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTime.text = formatTime(notification.createdAt)

            // Set icon based on type - using built-in Android icons
            val iconRes = when (notification.type) {
                "report_status" -> android.R.drawable.ic_menu_report_image
                "document_status" -> android.R.drawable.ic_menu_agenda
                "points_awarded" -> android.R.drawable.star_big_on
                else -> android.R.drawable.ic_dialog_info
            }
            binding.ivIcon.setImageResource(iconRes)

            // Visual indicator for unread notifications
            if (!notification.isRead) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.notification_unread_bg)
                )
                binding.viewUnreadIndicator.visibility = View.VISIBLE
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
                binding.viewUnreadIndicator.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(notification)
            }
        }

        private fun formatTime(timestamp: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(timestamp)
                val now = Date()
                val diff = now.time - (date?.time ?: 0)

                when {
                    diff < 60000 -> "Just now"
                    diff < 3600000 -> "${diff / 60000}m ago"
                    diff < 86400000 -> "${diff / 3600000}h ago"
                    diff < 604800000 -> "${diff / 86400000}d ago"
                    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                timestamp
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
        override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem == newItem
        }
    }
}
