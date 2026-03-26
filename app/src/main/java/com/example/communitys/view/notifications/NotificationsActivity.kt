package com.example.communitys.view.notifications

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communitys.SupabaseAuthHelper
import com.example.communitys.databinding.ActivityNotificationsBinding
import com.example.communitys.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var viewModel: NotificationViewModel
    private lateinit var adapter: NotificationsAdapter
    private val authHelper = SupabaseAuthHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                lifecycleScope.launch {
                    val userId = authHelper.getCurrentUserId()
                    if (userId != null && !notification.isRead) {
                        viewModel.markAsRead(notification.id, userId)
                    }
                }
            },
            onDeleteClick = { notification ->
                lifecycleScope.launch {
                    val userId = authHelper.getCurrentUserId()
                    if (userId != null) {
                        viewModel.deleteNotification(notification.id, userId)
                    }
                }
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnMarkAllRead.setOnClickListener {
            lifecycleScope.launch {
                val userId = authHelper.getCurrentUserId()
                if (userId != null) {
                    viewModel.markAllAsRead(userId)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(this) { notifications ->
            adapter.submitList(notifications)
            if (notifications.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.unreadCount.observe(this) { count ->
            binding.btnMarkAllRead.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            val userId = authHelper.getCurrentUserId()
            if (userId != null) {
                viewModel.loadNotifications(userId)
                viewModel.loadUnreadCount(userId)
            }
        }
    }
}
